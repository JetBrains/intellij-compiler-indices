package org.jetbrains.sbt.indices

import java.io.File
import java.util.UUID

final case class CompilationResult(
  successful:      Boolean,
  finishTimestamp: Long,
  compilationId:   UUID,
  infoFile:        Option[File]
) {
  if (successful) require(infoFile.nonEmpty)
}
