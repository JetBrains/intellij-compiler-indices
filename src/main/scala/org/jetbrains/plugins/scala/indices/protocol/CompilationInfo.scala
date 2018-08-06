package org.jetbrains.plugins.scala.indices.protocol

import java.io.File

trait CompilationInfo {
  def affectedModules: Set[String]
  def removedSources: Set[File]
  def generatedClasses: Set[CompiledClass]
  def startTimestamp: Long
}
