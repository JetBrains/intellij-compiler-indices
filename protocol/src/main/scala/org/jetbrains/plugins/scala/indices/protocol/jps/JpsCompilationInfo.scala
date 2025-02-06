package org.jetbrains.plugins.scala.indices.protocol.jps

import org.jetbrains.plugins.scala.indices.protocol.{CompilationInfo, CompiledClass}

import java.nio.file.Path

final case class JpsCompilationInfo(
  affectedModules:               Set[String],
  override val removedSources:   Set[Path],
  override val generatedClasses: Set[CompiledClass],
  override val startTimestamp:   Long
) extends CompilationInfo
