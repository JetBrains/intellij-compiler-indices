package org.jetbrains.plugins.scala.indices.protocol

import java.nio.file.Path

package object sbt {
  def compilationInfoDirName: String    = ".sbt-compilation-infos"
  def compilationInfoFilePrefix: String = "compilation-info"
  def ideaACK: String                   = "ack"

  def compilationInfoBaseDir(thisBuildBase: Path): Path =
    thisBuildBase.resolve(s"project/target/$compilationInfoDirName")

  def projectCompilationInfoDir(thisBuildBase: Path, projectId: String): Path =
    compilationInfoBaseDir(thisBuildBase).resolve(projectId)
}
