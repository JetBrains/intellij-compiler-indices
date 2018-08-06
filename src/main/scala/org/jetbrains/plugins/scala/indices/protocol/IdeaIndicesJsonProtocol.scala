package org.jetbrains.plugins.scala.indices.protocol

import org.jetbrains.plugins.scala.indices.protocol.jps.JpsCompilationInfoJsonProtocol
import org.jetbrains.plugins.scala.indices.protocol.sbt.SbtCompilationInfoJsonProtocol

trait IdeaIndicesJsonProtocol
    extends CompiledClassJsonProtocol
    with SbtCompilationInfoJsonProtocol
    with JpsCompilationInfoJsonProtocol

object IdeaIndicesJsonProtocol extends IdeaIndicesJsonProtocol {
  val ACK: String = "ACK"
}
