package org.jetbrains.plugins.scala.indices.protocol.sbt
import java.io.File

import org.jetbrains.plugins.scala.indices.protocol.{CompilationInfo, CompiledClass}

final case class SbtCompilationInfo(
  project: String,
  buildBaseDir: String,
  isIncremental: Boolean,
  scalaVersion: String,
  finishTimestamp: Long,
  override val removedSources: Set[File],
  override val generatedClasses: Set[CompiledClass]
) extends CompilationInfo {
  override def affectedModules: Set[String] = Set(project)
}
