package org.jetbrains.plugins.scala.indices.protocol

import spray.json._

trait CompiledClassJsonProtocol extends DomainDefaultJsonProtocol {
  implicit val compiledClassJsonFormat: JsonFormat[CompiledClass] = jsonFormat2(CompiledClass)
}

object CompiledClassJsonProtocol extends CompiledClassJsonProtocol
