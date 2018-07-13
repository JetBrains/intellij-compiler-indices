package org.jetbrains.sbt.indices

import java.io._
import java.net.Socket
import java.util.{Optional, UUID}

import org.jetbrains.plugins.scala.indices.protocol.CompiledClass
import org.jetbrains.plugins.scala.indices.protocol.IdeaIndicesJsonProtocol.ACK
import org.jetbrains.plugins.scala.indices.protocol.IdeaIndicesJsonProtocol._
import org.jetbrains.plugins.scala.indices.protocol.sbt.SbtCompilationInfo
import sbt.Keys._
import sbt.internal.inc.Analysis
import sbt.plugins.{CorePlugin, JvmPlugin}
import sbt.{AutoPlugin, Def, _}
import spray.json._
import xsbti.compile._

import scala.util.Try

object SbtIntellijIndicesPlugin extends AutoPlugin {
  import org.jetbrains.sbt.indices.IntelliJIndexer._

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = CorePlugin && JvmPlugin

  object autoImport {
    lazy val incrementalityType = settingKey[IncrementalityType]("Internal use: Configures index incrementality type")
    lazy val ideaPort           = settingKey[Int]("Port to talk to IDEA indexer")
  }
  import autoImport._

  private[this] def perConfig: Seq[Def.Setting[_]] = Seq(
    incrementalityType := IncrementalityType.Incremental,
    cleanKeepFiles     += target.value / compilationInfoDir,
    manipulateBytecode := Def.taskDyn {
      val compilationId = UUID.randomUUID()
      val version       = scalaVersion.value
      val itype         = incrementalityType.value
      val project       = thisProject.value.id
      val buildBaseDir  = (baseDirectory in ThisBuild).value
      val port          = ideaPort.value
      val socket        = notifyIdeaStart(port, compilationId)
      val targetDir     = target.value

      val previousResult = itype match {
        case IncrementalityType.Incremental    => previousCompile.value
        case IncrementalityType.NonIncremental => PreviousResult.create(Optional.empty(), Optional.empty())
      }

      Def.task {
        val compilationFinishTimestamp = System.currentTimeMillis() // this is actually executed after actual compilation

        val oldTaskValue = compileIncremental.result.value match {
          case Inc(inc) =>
            socket.foreach { s =>
              val result = CompilationResult(successful = false, compilationFinishTimestamp, compilationId, None)
              notifyFinish(s, result)
              s.close()
            }
            throw inc
          case Value(v) => v
        }

        try {
          //@TODO: what if info dumping fails while IDEA is not listening for connections
          val compilationInfoFile = dumpCompilationInfo(
            oldTaskValue.analysis(),
            previousResult,
            project,
            buildBaseDir,
            version,
            itype,
            targetDir / compilationInfoDir,
            compilationId
          )
          val result = CompilationResult(successful = true, compilationFinishTimestamp, compilationId, Option(compilationInfoFile))
          socket.foreach(notifyFinish(_, result))
          oldTaskValue
        } finally socket.foreach(_.close())
      }
    }.value
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(perConfig) ++ inConfig(Test)(perConfig)

  override def globalSettings: Seq[Def.Setting[_]] = Seq(ideaPort := 65337)
}

object IntelliJIndexer {
  val compilationInfoDir: String = "idea-compilation-info"

  def notifyIdeaStart(port: Int, compilationId: UUID): Option[Socket] = {
    val socket = Try(new Socket("localhost", port))

    socket.toOption.map { s =>
      val in = new DataInputStream(s.getInputStream())
      val ack = in.readUTF()
      if (ack != ACK) throw new RuntimeException("Malformed response from IDEA.")
      s
    }
  }

  def notifyFinish(socket: Socket, result: CompilationResult): Unit =
    try {
      val out = new DataOutputStream(socket.getOutputStream())
      out.writeBoolean(result.successful)
      out.writeLong(result.finishTimestamp)
      out.writeUTF(result.compilationId.toString)
      result.infoFile.foreach(f => out.writeUTF(f.getPath))
    } finally socket.close()

  sealed trait IncrementalityType
  object IncrementalityType {
    final case object Incremental    extends IncrementalityType
    final case object NonIncremental extends IncrementalityType
  }

  def dumpCompilationInfo(
    canalysis:          CompileAnalysis,
    prev:               PreviousResult,
    project:            String,
    buildBaseDir:       File,
    scalaVersion:       String,
    incrementalityType: IncrementalityType,
    compilationInfoDir: File,
    compilationId:      UUID
  ): File = {
    val analysis = canalysis.asInstanceOf[Analysis]
    val prevAnalysis = prev.analysis().orElse(Analysis.Empty).asInstanceOf[Analysis]

    val currentStamps = analysis.stamps
    val relations     = analysis.relations
    val oldStamps     = prevAnalysis.stamps

    val generatedClasses: Set[CompiledClass] =
      currentStamps.products.collect {
        case (file, stamp) if oldStamps.product(file) != stamp =>
          val fromSource = relations.produced(file).head
          CompiledClass(fromSource, file)
      }(collection.breakOut)

    val deletedSources = oldStamps.allSources -- currentStamps.allSources
    val isIncremental  = incrementalityType == IncrementalityType.Incremental

    val compilationInfo = SbtCompilationInfo(
      project,
      buildBaseDir.getPath,
      isIncremental,
      scalaVersion,
      timestamp,
      deletedSources.toSet,
      generatedClasses
    )

    val compilationInfoFile = compilationInfoDir / s"sbt-compilation-info-${compilationId.toString}"
    val out                 = new PrintWriter(new BufferedWriter(new FileWriter(compilationInfoFile)))
    out.print(compilationInfo.toJson.compactPrint)
    out.close()
    compilationInfoFile
  }
}
