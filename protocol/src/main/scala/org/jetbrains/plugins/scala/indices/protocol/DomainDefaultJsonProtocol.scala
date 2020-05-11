package org.jetbrains.plugins.scala.indices.protocol

import java.io.File

import spray.json._

trait DomainDefaultJsonProtocol extends DefaultJsonProtocol {
  implicit val fileJsonFormat: JsonFormat[File] = new JsonFormat[File] {
    override def read(json: JsValue): File = json match {
      case JsString(path) => new File(path)
      case x              => deserializationError("Expected file path as JsString, but got " + x)
    }
    override def write(file: File): JsValue = file.getPath.toJson
  }
}
