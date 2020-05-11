package org.jetbrains.sbt.indices

import java.io.File
import java.util
import java.util.concurrent.ConcurrentHashMap

import org.jetbrains.sbt.indices.IntellijIndexer.ClassesInfo
import org.jetbrains.sbt.indices.SbtCompilationBackCompat._

private class IndexingClassfileManager(inherited: ClassFileManager) extends ClassFileManager {
  import IndexingClassfileManager._

  override def delete(classes: Iterable[File]): Unit = {
    deletedStaging.set(classes.toArray)
    inherited.delete(classes)
  }

  override def generated(classes: Iterable[File]): Unit = {
    generatedStaging.set(classes.toArray)
    inherited.generated(classes)
  }

  override def complete(success: Boolean): Unit = {
    if (success) classesInfo.add(ClassesInfo(generatedStaging.get, deletedStaging.get))

    generatedStaging.remove()
    deletedStaging.remove()
    inherited.complete(success)
  }
}

object IndexingClassfileManager {
  val classesInfo: util.Set[ClassesInfo] = ConcurrentHashMap.newKeySet[ClassesInfo]

  val generatedStaging = new ThreadLocal[Array[File]] {
    override def initialValue(): Array[File] = Array.empty
  }

  val deletedStaging   = new ThreadLocal[Array[File]] {
    override def initialValue(): Array[File] = Array.empty
  }

  def apply(inherited: ClassFileManager): ClassFileManager =
    new IndexingClassfileManager(inherited)
}