package org.jetbrains.plugins.scala.indices.protocol.sbt

import java.io.{File, IOException, RandomAccessFile}
import java.nio.channels.FileLock
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

object Locking {
  private[this] final case class LockData(raf: RandomAccessFile, lock: FileLock)
  private[this] val locks: ConcurrentMap[File, LockData] = new ConcurrentHashMap[File, LockData]

  private[this] type Logger = String => Unit
  private[this] val noopLogger: Logger = Function.const(())

  def lockFile(compilationInfoDir: File): File =
    new File(compilationInfoDir, ".sbt-idea-lock")

  def lock(lockFile: File)(log: Logger = noopLogger): FileLock = {
    if (lockFile.getParentFile.exists() || lockFile.getParentFile.mkdirs()) {
      val raf = new RandomAccessFile(lockFile, "rw")

      try {
        log(s"Acquiring lock on file $lockFile ...")
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

  def unlock(lockFile: File)(log: Logger = noopLogger): Unit = {
    val data = locks.get(lockFile)
    if (data == null) {
      throw new IllegalArgumentException(s"Trying to unlock non-locked file $lockFile.")
    } else
      try {
        locks.remove(data)
        log(s"Releasing lock on file $lockFile.")
        data.lock.release()
        data.raf.close()
      } catch { case e: IOException => e.printStackTrace() }
  }

  implicit class FileLockingExt(val dir: File) extends AnyVal {
    def lock(log: Logger = noopLogger): Unit = {
      val lockF = lockFile(dir)
      Locking.lock(lockF)(log)
    }

    def unlock(log: Logger = noopLogger): Unit = {
      val lockF = lockFile(dir)
      Locking.unlock(lockF)(log)
    }

    def withLockInDir[R](log: Logger = noopLogger)(body: => R): R = {
      lock(log)
      try body
      finally unlock(log)
    }
  }
}
