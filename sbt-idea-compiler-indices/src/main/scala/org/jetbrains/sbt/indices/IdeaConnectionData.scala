package org.jetbrains.sbt.indices

import org.jetbrains.sbt.indices.IntellijIndexer.IncrementalityType
import sbt.Configuration

import java.io.File
import java.net.Socket
import java.util.UUID

private[indices] final case class IdeaConnectionData(
  socket: Option[Socket],
  projectId: String,
  configurationId: Configuration,
  compilationId: UUID,
  itype: IncrementalityType,
  version: String,
  infoDir: File,
  compilationStartTimestamp: Long
)
