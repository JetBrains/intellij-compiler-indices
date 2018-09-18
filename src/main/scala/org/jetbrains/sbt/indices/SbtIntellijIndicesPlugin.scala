package org.jetbrains.sbt.indices

import java.io._
import java.net.Socket
import java.nio.file.Files
import java.util.UUID

import org.jetbrains.plugins.scala.indices.protocol.CompiledClass
import org.jetbrains.plugins.scala.indices.protocol.IdeaIndicesJsonProtocol._
import org.jetbrains.plugins.scala.indices.protocol.sbt.Locking._
import org.jetbrains.plugins.scala.indices.protocol.sbt._
import org.jetbrains.sbt.indices.SbtCompilationBackCompat._
import sbt.Keys._
import sbt.plugins.{CorePlugin, JvmPlugin}
import sbt.{AutoPlugin, Def, _}
import spray.json._

import scala.collection.JavaConverters._
import scala.util.Try

object SbtIntellijIndicesPlugin extends AutoPlugin {
  import org.jetbrains.sbt.indices.IntellijIndexer._

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = CorePlugin && JvmPlugin

  object autoImport {
    lazy val incrementalityType =
      settingKey[IncrementalityType]("Internal use only: Configures index incrementality type")
    lazy val ideaPort = settingKey[Int]("Port to talk to IDEA indexer")
  }
  import autoImport._

  private[this] def perConfig: Seq[Def.Setting[_]] = Seq(
    incOptions ~= { opt => opt.withClassfileManager(IndexingClassfileManager(opt))
    },
    compile := Def.taskDyn {
      val previousValue = compile.taskValue
      val log           = streams.value.log
      val buildBaseDir  = (baseDirectory in ThisBuild).value

      if (!isIdeaProject(buildBaseDir)) Def.task(previousValue.value)
      else {
        val projectId     = thisProject.value.id
        val compilationId = UUID.randomUUID()
        val itype         = incrementalityType.value
        val version       = scalaBinaryVersion.value
        val infoDir       = compilationInfoDir(buildBaseDir, projectId)
        val port          = ideaPort.value

        infoDir.lock(log = log.info(_))
        val socket                    = notifyIdeaStart(port, buildBaseDir.getPath, compilationId)
        val compilationStartTimestamp = System.currentTimeMillis()

        Def.taskDyn {
          val previousResult = itype match {
            case IncrementalityType.Incremental    => previousCompile.value
            case IncrementalityType.NonIncremental => PreviousResult.empty()
          }

          Def.task {
            val oldTaskValue = previousValue.result.value match {
              case Inc(inc) =>
                socket.foreach { s =>
                  val result = CompilationResult(successful = false, compilationStartTimestamp, None)
                  notifyFinish(s, result)
                  s.close()
                }
                infoDir.unlock(log = log.info(_))
                throw inc
              case Value(v) => v
            }

            try {
              //@TODO: what if info dumping fails while IDEA is not listening for connections
              val compilationInfoFile = dumpCompilationInfo(
                oldTaskValue,
                previousResult,
                projectId,
                version,
                itype,
                infoDir,
                compilationStartTimestamp,
                compilationId
              )
              val result = CompilationResult(
                successful = true,
                compilationStartTimestamp,
                Option(compilationInfoFile)
              )
              socket.foreach(notifyFinish(_, result))
              oldTaskValue
            } finally {
              socket.foreach(_.close())
              infoDir.unlock(log = log.info(_))
            }
          }
        }
      }
    }.value
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(perConfig) ++ inConfig(Test)(perConfig) ++ Seq(
      cleanFiles += {
        val base = (baseDirectory in ThisBuild).value
        compilationInfoDir(base, thisProject.value.id)
      }
    )

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    ideaPort           := 65337,
    incrementalityType := IncrementalityType.Incremental
  )
}

object IntellijIndexer {
  final case class ClassesInfo(generated: Array[File], deleted: Array[File])

  object ClassesInfo {
    def empty: ClassesInfo = ClassesInfo(Array.empty[File], Array.empty[File])
  }

  def compilationInfoDir(base: File, projectId: String): File =
    projectCompilationInfoDir(base, projectId).toFile

  def isIdeaProject(base: File): Boolean = Files.isDirectory(base.toPath.resolve(".idea"))

  def findCorrespondingClassesInfo(
    currentRelations: Relations,
    prevRelations:    Relations
  ): ClassesInfo = {
    val result = IndexingClassfileManager.classesInfo.asScala.find { info =>
      def isInCurrent = info.generated.headOption.exists(currentRelations.allProducts.contains)
      def isInPrev    = info.deleted.headOption.exists(prevRelations.allProducts.contains)

      isInCurrent || isInPrev
    }

    result.foreach(IndexingClassfileManager.classesInfo.remove)
    result.getOrElse(ClassesInfo.empty)
  }

  def dumpCompilationInfo(
    canalysis:          CompileAnalysis,
    prev:               PreviousResult,
    projectId:          String,
    scalaVersion:       String,
    incrementalityType: IncrementalityType,
    compilationInfoDir: File,
    timestamp:          Long,
    compilationId:      UUID
  ): File = {
    val analysis     = canalysis.asInstanceOf[Analysis]
    val prevAnalysis = prev.getAnalysis.orElse(Analysis.Empty).asInstanceOf[Analysis]

    val prevRelations = prevAnalysis.relations
    val relations     = analysis.relations

    val classesInfo   = findCorrespondingClassesInfo(relations, prevRelations)
    val isIncremental =
      incrementalityType != IncrementalityType.NonIncremental && // for builds forced from inside the IDEA
        classesInfo.generated.length != relations.allProducts.size // for regular clean builds

    val generatedClasses: Set[CompiledClass] = {
      val classes =
        if (isIncremental) classesInfo.generated.toSet
        else               relations.allProducts

      classes.map(
        f => CompiledClass(relations.produced(f).head, f)
      )(collection.breakOut)
    }

    val deletedSources: Set[File] =
      if (isIncremental)
        classesInfo.deleted
          .map(prevRelations.produced(_).head)(collection.breakOut)
      else Set.empty

    val compilationInfo = SbtCompilationInfo(
      projectId,
      isIncremental,
      scalaVersion,
      deletedSources,
      generatedClasses,
      timestamp
    )

    val compilationInfoFile = compilationInfoDir / s"$compilationInfoFilePrefix-${compilationId.toString}"
    compilationInfoDir.mkdirs()
    val out = new PrintWriter(new BufferedWriter(new FileWriter(compilationInfoFile)))
    out.print(compilationInfo.toJson.compactPrint)
    out.close()

    compilationInfoFile
  }

  def notifyIdeaStart(port: Int, projectBase: String, compilationId: UUID): Option[Socket] = {
    val socket = Try(new Socket("localhost", port))

    socket.toOption.map { s =>
      val in  = new DataInputStream(s.getInputStream())
      val out = new DataOutputStream(s.getOutputStream())
      out.writeUTF(projectBase)
      out.writeUTF(compilationId.toString)
      val ack = in.readUTF()
      if (ack != ideaACK) throw new RuntimeException("Malformed response from IDEA.")
      s
    }
  }

  def notifyFinish(socket: Socket, result: CompilationResult): Unit =
    try {
      val in  = new DataInputStream(socket.getInputStream())
      val out = new DataOutputStream(socket.getOutputStream())
      out.writeBoolean(result.successful)
      result.infoFile.foreach(f => out.writeUTF(f.getPath))
      val ack = in.readUTF()
      if (ack != ideaACK) throw new RuntimeException("Malformed response from IDEA.")
    } finally socket.close()

  sealed trait IncrementalityType
  object IncrementalityType {
    final case object Incremental    extends IncrementalityType
    final case object NonIncremental extends IncrementalityType
  }
}
