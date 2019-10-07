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

import org.apache.commons.compress.archivers.tar._
import java.io.{ BufferedInputStream, BufferedOutputStream, File, FileInputStream, FileOutputStream, IOException }
import java.nio.file.{ Files, LinkOption, Path, Paths, StandardCopyOption }
import java.util

import collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object Tar {

  private object Mode {
    val EXEC_MODE = 1 + 8 + 64
    val WRITE_MODE = 2 + 16 + 128
    val READ_MODE = 4 + 32 + 256
  }

  import Mode._

  def archive(directory: File, archive: File, includeDirectoryName: Boolean = false) = {
    val tos = new TarArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(archive)))
    try {

      if (!Files.isDirectory(directory.toPath)) throw new IOException(directory.toString + " is not a directory.")

      val toArchive = new util.Stack[(File, String)]
      if (!includeDirectoryName) toArchive.push(directory → "") else toArchive.push(directory → directory.getName)

      while (!toArchive.isEmpty) {
        val (source, entryName) = toArchive.pop
        val isSymbolicLink = Files.isSymbolicLink(source.toPath)
        val isDirectory = Files.isDirectory(source.toPath)

        // tar structure distinguishes symlinks
        val e =
          if (isDirectory && !isSymbolicLink) {
            // walk the directory tree to add all its entries to stack
            val stream = Files.newDirectoryStream(source.toPath)
            try {
              for (f ← stream.asScala) {
                val newSource = source.toPath.resolve(f.getFileName)
                val newEntryName = entryName + '/' + f.getFileName
                toArchive.push((newSource.toFile, newEntryName))
              }
            } finally stream.close()

            // create the actual tar entry for the directory
            new TarArchiveEntry(entryName + '/')
          } // tar distinguishes symlinks
          else if (isSymbolicLink) {
            val e = new TarArchiveEntry(entryName, TarConstants.LF_SYMLINK)
            e.setLinkName(Files.readSymbolicLink(source.toPath).toString)
            e
          } // plain files
          else {
            val e = new TarArchiveEntry(entryName)
            e.setSize(Files.size(source.toPath))
            e
          }

        def mode(source: Path) = {
          val f = source.toRealPath()

          (if (Files.isReadable(f)) READ_MODE else 0) |
            (if (Files.isWritable(f)) WRITE_MODE else 0) |
            (if (Files.isExecutable(f)) EXEC_MODE else 0)
        }

        // complete current entry by fixing its modes and writing it to the archive
        if (source != directory) {
          if (!isSymbolicLink) e.setMode(mode(source.toPath))
          tos.putArchiveEntry(e)
          if (Files.isRegularFile(source.toPath, LinkOption.NOFOLLOW_LINKS)) try Files.copy(source.toPath, tos)
          finally tos.closeArchiveEntry()
        }
      }
    } finally tos.close()
  }

  def extract(archive: File, directory: File, overwrite: Boolean = false) = {
    /** set mode from an integer as retrieved from a Tar archive */
    def setMode(file: Path, m: Int) = {
      val f = file.toRealPath().toFile
      f.setReadable((m & READ_MODE) != 0)
      f.setWritable((m & WRITE_MODE) != 0)
      f.setExecutable((m & EXEC_MODE) != 0)
    }

    val tis = new TarArchiveInputStream(new BufferedInputStream(new FileInputStream(archive)))
    try {
      if (!directory.exists()) directory.mkdirs()
      if (!Files.isDirectory(directory.toPath)) throw new IOException(directory.toString + " is not a directory.")

      val directoryRights = ListBuffer[(Path, Int)]()

      Iterator.continually(tis.getNextTarEntry).takeWhile(_ != null).foreach {
        e ⇒
          val dest = Paths.get(directory.toString, e.getName)
          if (e.isDirectory) {
            Files.createDirectories(dest)
            directoryRights += (dest -> e.getMode)
          } else {
            Files.createDirectories(dest.getParent)

            // has the entry been marked as a symlink in the archive?
            if (!e.getLinkName.isEmpty) Files.createSymbolicLink(dest, Paths.get(e.getLinkName))
            // file copy from an InputStream does not support COPY_ATTRIBUTES, nor NOFOLLOW_LINKS
            else {
              Files.copy(tis, dest, Seq(StandardCopyOption.REPLACE_EXISTING).filter { _ ⇒ overwrite }: _*)
              setMode(dest, e.getMode)
            }
          }
      }

      // Set directory right after extraction in case some directory are not writable
      for {
        (path, mode) ← directoryRights
      } setMode(path, mode)
    } finally tis.close()
  }

}
