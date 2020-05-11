package org.jetbrains.plugins.scala.indices.protocol.jps

import org.jetbrains.plugins.scala.indices.protocol.CompiledClassJsonProtocol
import spray.json.JsonFormat

trait JpsCompilationInfoJsonProtocol extends CompiledClassJsonProtocol {
  implicit val jpsChunkBuildDataJsonFormat: JsonFormat[JpsCompilationInfo] = jsonFormat4(JpsCompilationInfo)
}

object JpsCompilationInfoJsonProtocol extends JpsCompilationInfoJsonProtocol
