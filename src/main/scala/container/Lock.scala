package container

import java.io.{ BufferedOutputStream, FileOutputStream, OutputStream }
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

import scala.collection.mutable

/*
 * Copyright (C) 2019 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
object lock {

  object LockRepository {
    def apply[T]() = new LockRepository[T]()
  }

  class LockRepository[T] {

    val locks = new mutable.HashMap[T, (ReentrantLock, AtomicInteger)]

    def nbLocked(k: T) = locks.synchronized(locks.get(k).map { case (_, users) ⇒ users.get }.getOrElse(0))

    def lock(obj: T) = locks.synchronized {
      val (lock, users) = locks.getOrElseUpdate(obj, (new ReentrantLock, new AtomicInteger(0)))
      users.incrementAndGet
      lock
    }.lock()

    def unlock(obj: T) = locks.synchronized {
      locks.get(obj) match {
        case Some((lock, users)) ⇒
          val value = users.decrementAndGet
          if (value <= 0) locks.remove(obj)
          lock
        case None ⇒ throw new IllegalArgumentException("Unlocking an object that has not been locked.")
      }
    }.unlock()

    def withLock[A](obj: T)(op: ⇒ A) = {
      lock(obj)
      try op
      finally unlock(obj)
    }

  }

  val jvmLevelFileLock = new LockRepository[String]

  def withLock[T](file: java.io.File)(f: OutputStream ⇒ T): T = jvmLevelFileLock.withLock(file.getCanonicalPath) {
    autoClose(new FileOutputStream(file, true)) { fos ⇒
      autoClose(new BufferedOutputStream(fos)) { bfos ⇒
        val lock = fos.getChannel.lock
        try f(bfos)
        finally lock.release
      }
    }
  }

  def withLockInDirectory[T](file: java.io.File)(f: ⇒ T, lockName: String = ".lock"): T = {
    import better.files._
    val lockFile = (file.toScala / lockName).toJava
    lockFile.createNewFile()
    try withLock(lockFile) { _ ⇒ f }
    finally lockFile.delete()
  }

  private def autoClose[A <: AutoCloseable, B](closeable: A)(fun: (A) ⇒ B): B = {
    var t: Throwable = null
    try {
      fun(closeable)
    } catch {
      case funT: Throwable ⇒
        t = funT
        throw t
    } finally {
      if (t != null) {
        try {
          closeable.close()
        } catch {
          case closeT: Throwable ⇒
            t.addSuppressed(closeT)
            throw t
        }
      } else {
        closeable.close()
      }
    }
  }

}
