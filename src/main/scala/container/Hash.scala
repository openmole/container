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

package container

import java.io.{BufferedInputStream, FileInputStream, InputStream}
import java.security.MessageDigest

import squants.information.Information
import squants.information.InformationConversions._

object Hash {

  object HashSource {
    case class FileSource(f: java.io.File) extends HashSource
    case class InputStreamSource(is: InputStream) extends HashSource
    case class StringSource(s: String) extends HashSource

    implicit def fromFile(f: java.io.File) = FileSource(f)
    implicit def fromString(s: String) = StringSource(s)
    implicit def fromInputString(is: InputStream) = InputStreamSource(is)
  }

  sealed trait HashSource

  def sha256(source: HashSource, bufferSize: Information = 4 megabytes): String = {

    def hashInputStream(is: InputStream) = {
      val buffer = new Array[Byte](bufferSize.toBytes.toInt)
      val md = MessageDigest.getInstance("SHA-256")

      Iterator.continually(is.read(buffer)).takeWhile(_ != -1).foreach {
        count ⇒ md.update(buffer, 0, count)
      }

      String.format("%064x", new java.math.BigInteger(1, md.digest))
    }

    source match {
      case HashSource.InputStreamSource(is) => hashInputStream(is)
      case HashSource.StringSource(s) =>
        String.format("%064x", new java.math.BigInteger(1, java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"))))
      case HashSource.FileSource(f) =>
        val is = new BufferedInputStream(new FileInputStream(f))
        try hashInputStream(is)
        finally is.close
    }
  }

}
