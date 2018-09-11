package org.jetbrains.plugins.scala.indices.protocol.sbt

import java.io.File

import org.jetbrains.plugins.scala.indices.protocol.{CompilationInfo, CompiledClass}

final case class SbtCompilationInfo(
  projectId:                     String,
  isIncremental:                 Boolean,
  scalaVersion:                  String,
  override val removedSources:   Set[File],
  override val generatedClasses: Set[CompiledClass],
  override val startTimestamp:   Long
) extends CompilationInfo
