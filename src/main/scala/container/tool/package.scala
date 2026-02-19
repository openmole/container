package container.tool

import java.io.{ OutputStream, PrintStream }

import scala.sys.process.ProcessLogger
import java.nio.file.*
import java.nio.file.StandardCopyOption.*
import scala.jdk.CollectionConverters.*

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



def outputLogger = System.out
def nullLogger = new PrintStream(new OutputStream {
  override def write(i: Int): Unit = {}
})

def copyOrMerge(src: Path, dest: Path): Unit =
  if Files.isSymbolicLink(src)
  then
    // copy symlink AS symlink (even if broken)
    dest.getParent.toFile.mkdirs()
    val linkTarget = Files.readSymbolicLink(src)
    Files.deleteIfExists(dest)
    Files.createSymbolicLink(dest, linkTarget)
  else if Files.isDirectory(src, LinkOption.NOFOLLOW_LINKS) then
    dest.toFile.mkdirs()
    val stream = Files.list(src)
    try
      for child <- stream.iterator().asScala do
        copyOrMerge(child, dest.resolve(child.getFileName))
    finally
      stream.close()
  else
    dest.getParent.toFile.mkdirs()
    Files.copy(
      src,
      dest,
      REPLACE_EXISTING,
      LinkOption.NOFOLLOW_LINKS
    )
