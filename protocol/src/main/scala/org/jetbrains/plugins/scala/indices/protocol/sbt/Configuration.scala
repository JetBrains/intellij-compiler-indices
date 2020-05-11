package org.jetbrains.plugins.scala.indices.protocol.sbt

import spray.json._

sealed trait Configuration

object Configuration {
  val allConfigurations: Set[Configuration] = Set(Test, Compile)

  final case object Test    extends Configuration
  final case object Compile extends Configuration

  implicit val configurationFormat: JsonFormat[Configuration] = new JsonFormat[Configuration] {
    override def read(json: JsValue): Configuration = json match {
      case JsString("Compile") => Compile
      case JsString("Test")    => Test
      case _                   => deserializationError("Compile or Test configuration expected.")
    }

    override def write(conf: Configuration): JsValue = JsString(conf.toString)
  }
}
