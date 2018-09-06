package org.jetbrains.plugins.scala.indices.protocol.sbt

import org.jetbrains.plugins.scala.indices.protocol.CompiledClassJsonProtocol
import spray.json.JsonFormat

trait SbtCompilationInfoJsonProtocol extends CompiledClassJsonProtocol {
  implicit val sbtCompilationInfoJsonFormat: JsonFormat[SbtCompilationInfo] = jsonFormat6(SbtCompilationInfo.apply)
}

object SbtCompilationInfoJsonProtocol extends SbtCompilationInfoJsonProtocol
