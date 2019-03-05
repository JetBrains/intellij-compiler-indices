package org.jetbrains.sbt.indices

import java.io.File

final case class CompilationResult(
  successful:     Boolean,
  startTimestamp: Long,
  infoFile:       Option[File]
)
