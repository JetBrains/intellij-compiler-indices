package org.jetbrains.plugins.scala.indices.protocol

import spray.json._

import java.nio.file.{Path, Paths}

trait DomainDefaultJsonProtocol extends DefaultJsonProtocol {
  implicit val pathJsonFormat: JsonFormat[Path] = new JsonFormat[Path] {
    override def read(json: JsValue): Path = json match {
      case JsString(path) => Paths.get(path)
      case x              => deserializationError("Expected file path as JsString, but got " + x)
    }

    override def write(path: Path): JsValue = path.toAbsolutePath.normalize().toString.toJson
  }
}
