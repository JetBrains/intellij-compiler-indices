package org.jetbrains.plugins.scala.indices.protocol.sbt

import java.io.{File, IOException, RandomAccessFile}
import java.nio.channels.FileLock
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

object Locking {
  private[this] final case class LockData(raf: RandomAccessFile, lock: FileLock)

  private[this] val locks: ConcurrentMap[File, LockData] = new ConcurrentHashMap[File, LockData]

  def lockFile(compilationInfoDir: File): File =
    new File(compilationInfoDir, ".sbt-idea-lock")

  def lock(lockFile: File): FileLock = {
    if (lockFile.getParentFile.exists() || lockFile.getParentFile.mkdirs()) {
      val raf = new RandomAccessFile(lockFile, "rw")

      try {
        val lock = raf.getChannel.lock()
        locks.put(lockFile, LockData(raf, lock))
        lock
      }
      catch {
        case e: IOException =>
          raf.close()
          throw e
      }
    } else throw new RuntimeException("Unable to create lock file.")
  }

  def unlock(lockFile: File): Unit = {
    val data = locks.get(lockFile)
    if (data == null) {
      throw new IllegalArgumentException(s"Trying to unlock non-locked file $lockFile.")
    } else
      try {
        locks.remove(data)
        data.lock.release()
        data.raf.close()
      } catch { case e: IOException => e.printStackTrace() }
  }

  implicit class FileLockingExt(val dir: File) extends AnyVal {
    def lock(): Unit = {
      val lockF = lockFile(dir)
      Locking.lock(lockF)
    }

    def unlock(): Unit = {
      val lockF = lockFile(dir)
      Locking.unlock(lockF)
    }

    def withLockInDir[R](body: => R): R = {
      lock()
      try body
      finally unlock()
    }
  }
}
