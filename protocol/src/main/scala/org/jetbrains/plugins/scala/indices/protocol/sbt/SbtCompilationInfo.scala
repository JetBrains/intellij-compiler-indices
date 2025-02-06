package org.jetbrains.plugins.scala.indices.protocol.sbt

import org.jetbrains.plugins.scala.indices.protocol.{CompilationInfo, CompiledClass}

import java.nio.file.Path

final case class SbtCompilationInfo(
  projectId:                     String,
  isIncremental:                 Boolean,
  scalaVersion:                  String,
  configuration:                 Configuration,
  override val removedSources:   Set[Path],
  override val generatedClasses: Set[CompiledClass],
  override val startTimestamp:   Long
) extends CompilationInfo
