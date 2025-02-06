package org.jetbrains.plugins.scala.indices.protocol

import java.nio.file.Path

final case class CompiledClass(source: Path, output: Path)
