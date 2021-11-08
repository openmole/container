/*
 * Copyright (C) 2016 Vincent Hage
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

object Status {
  val DEBUG = false
  def note(message: => String) = if (DEBUG) println(message)

  sealed abstract class Status(
    val code: Int,
    val message: String) {
    var argument: Any = None

    def isBad() = code != 0
    def withArgument(arg: Any) = {
      this.argument = arg
      this
    }
    override def toString = {
      if (isBad()) {
        if (this.argument != None)
          Console.BOLD + Console.RED + "Error: " +
            this.message + " : " + this.argument +
            Console.RESET
        else
          Console.BOLD + Console.RED +
            "Error: " + this.message +
            Console.RESET
      } else
        "OK"
    }
  }

  case object UNKNOWN_ERROR extends Status(-1, "Unkown error")
  case object OK extends Status(0, "OK")
  case object INVALID_ARGUMENT extends Status(1, "Invalid argument")
  case object INVALID_PATH extends Status(2, "Invalid path")
  case object INVALID_IMAGE extends Status(3, "Invalid image")
  case object INVALID_IMAGE_FORMAT extends Status(4, "Issue with the image format")
  case object DIRECTORY_FILE_COLLISION extends Status(5, "File already exists at this path, and it's not a directory")
  case object SECURITY_ERROR extends Status(6, "Security exception")
  case object IO_ERROR extends Status(7, "IO exception")
}