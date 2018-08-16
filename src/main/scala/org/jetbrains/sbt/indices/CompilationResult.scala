package org.jetbrains.sbt.indices

import java.io.File
import java.util.UUID

final case class CompilationResult(
  successful:      Boolean,
  startTimestamp:  Long,
  compilationId:   UUID,
  infoFile:        Option[File]
) {
  if (successful) require(infoFile.nonEmpty)
}
