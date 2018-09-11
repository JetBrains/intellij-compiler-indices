package org.jetbrains.plugins.scala.indices.protocol

import java.io.File
import java.nio.file.Path

package object sbt {
  def compilationInfoDirName: String    = ".sbt-compilation-infos"
  def compilationInfoFilePrefix: String = "compilation-info"
  def ideaACK: String                   = "ack"

  def compilationInfoBaseDir(thisBuildBase: File): Path =
    thisBuildBase.toPath.resolve(".idea").resolve(compilationInfoDirName)

  def projectCompilationInfoDir(thisBuildBase: File, projectId: String): Path =
    compilationInfoBaseDir(thisBuildBase).resolve(projectId)
}
