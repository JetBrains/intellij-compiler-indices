package org.jetbrains.sbt.indices

import java.io.File
import java.util
import java.util.concurrent.ConcurrentHashMap

import org.jetbrains.sbt.indices.IntellijIndexer.ClassesInfo
import org.jetbrains.sbt.indices.SbtCompilationBackCompat._

private object IndexingClassfileManager extends ClassFileManager {
  val classesInfo: util.Set[ClassesInfo] = ConcurrentHashMap.newKeySet[ClassesInfo]

  private[this] val generatedStaging = new ThreadLocal[Array[File]]
  private[this] val deletedStaging   = new ThreadLocal[Array[File]]

  override def delete(classes:    Iterable[File]): Unit = deletedStaging.set(classes.toArray)
  override def generated(classes: Iterable[File]): Unit = generatedStaging.set(classes.toArray)

  override def complete(success: Boolean): Unit =
    if (success) classesInfo.add(ClassesInfo(generatedStaging.get, deletedStaging.get))
    else         { generatedStaging.remove(); deletedStaging.remove() }
}