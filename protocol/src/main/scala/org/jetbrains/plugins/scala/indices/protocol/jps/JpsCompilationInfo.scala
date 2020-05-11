package org.jetbrains.plugins.scala.indices.protocol.jps

import java.io.File

import org.jetbrains.plugins.scala.indices.protocol.{CompilationInfo, CompiledClass}

final case class JpsCompilationInfo(
  affectedModules:               Set[String],
  override val removedSources:   Set[File],
  override val generatedClasses: Set[CompiledClass],
  override val startTimestamp:   Long
) extends CompilationInfo
