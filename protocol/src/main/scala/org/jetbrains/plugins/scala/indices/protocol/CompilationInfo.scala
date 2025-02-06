package org.jetbrains.plugins.scala.indices.protocol

import java.nio.file.Path

trait CompilationInfo {
  def removedSources: Set[Path]
  def generatedClasses: Set[CompiledClass]
  def startTimestamp: Long

  def isEmpty: Boolean = removedSources.isEmpty && generatedClasses.isEmpty
}
