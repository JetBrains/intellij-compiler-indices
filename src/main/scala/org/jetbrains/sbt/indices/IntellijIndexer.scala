package org.jetbrains.sbt.indices

import java.io.{DataInputStream, DataOutputStream}
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.UUID

import org.jetbrains.plugins.scala.indices.protocol.CompiledClass
import org.jetbrains.plugins.scala.indices.protocol.IdeaIndicesJsonProtocol._
import org.jetbrains.plugins.scala.indices.protocol.sbt._
import org.jetbrains.sbt.indices.SbtCompilationBackCompat._
import sbt._
import spray.json._

import scala.collection.JavaConverters._
import scala.util.Try

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
      def isInPrev = info.deleted.headOption.exists(prevRelations.allProducts.contains)

      isInCurrent || isInPrev
    }

    result.foreach(IndexingClassfileManager.classesInfo.remove)
    result.getOrElse(ClassesInfo.empty)
  }

  def dumpCompilationInfo(
    isOffline:          Boolean,
    canalysis:          CompileAnalysis,
    prev:               PreviousResult,
    projectId:          String,
    scalaVersion:       String,
    incrementalityType: IncrementalityType,
    compilationInfoDir: File,
    timestamp:          Long,
    compilationId:      UUID
  ): File = {
    val analysis = canalysis.asInstanceOf[Analysis]
    val prevAnalysis = prev.getAnalysis.orElse(Analysis.Empty).asInstanceOf[Analysis]

    val prevRelations = prevAnalysis.relations
    val relations = analysis.relations

    val classesInfo = findCorrespondingClassesInfo(relations, prevRelations)
    val isIncremental =
      incrementalityType != IncrementalityType.NonIncremental && // for builds forced from inside the IDEA
        classesInfo.generated.length != relations.allProducts.size // for regular clean builds

    val generatedClasses: Set[CompiledClass] = {
      val classes =
        if (isIncremental) classesInfo.generated.toSet
        else relations.allProducts

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
    val out = Files.newBufferedWriter(compilationInfoFile.toPath, StandardCharsets.UTF_8)

    try     out.write(compilationInfo.toJson.compactPrint)
    finally out.close()

    compilationInfoFile
  }

  def notifyIdeaStart(port: Int, projectBase: String, compilationId: UUID): Option[Socket] = {
    val socket = Try(new Socket("localhost", port))

    socket.toOption.map { s =>
      try {
        val in = new DataInputStream(s.getInputStream())
        val out = new DataOutputStream(s.getOutputStream())
        out.writeUTF(projectBase)
        out.writeUTF(compilationId.toString)
        val ack = in.readUTF()
        if (ack != ideaACK) throw new RuntimeException("Malformed response from IDEA.")
        s
      } catch { case e: Throwable => s.close(); throw e }
    }
  }

  def notifyFinish(socket: Socket, result: CompilationResult): Unit = {
    val in = new DataInputStream(socket.getInputStream())
    val out = new DataOutputStream(socket.getOutputStream())
    out.writeBoolean(result.successful)
    result.infoFile.foreach(f => out.writeUTF(f.getPath))
    val ack = in.readUTF()
    if (ack != ideaACK) throw new RuntimeException("Malformed response from IDEA.")
  }

  sealed trait IncrementalityType
  object IncrementalityType {
    final case object Incremental    extends IncrementalityType
    final case object NonIncremental extends IncrementalityType
  }
}
