package container.tool

import org.apache.commons.compress.archivers.tar._
import java.io.{ BufferedInputStream, BufferedOutputStream, File, FileInputStream, FileOutputStream, IOException }
import java.nio.file.{ Files, LinkOption, Path, Paths, StandardCopyOption }
import java.util
import java.util.zip.GZIPInputStream

import collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object Tar {

  private object Mode:
    val EXEC_MODE = 1 + 8 + 64
    val WRITE_MODE = 2 + 16 + 128
    val READ_MODE = 4 + 32 + 256

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



  def extract(archive: File, directory: File, overwrite: Boolean = true, compressed: Boolean = false, filter: Option[TarArchiveEntry => Boolean] = None) = {
    /** set mode from an integer as retrieved from a Tar archive */
    def setMode(file: Path, m: Int) =
      val f = file.toRealPath().toFile
      f.setReadable((m & READ_MODE) != 0)
      f.setWritable((m & WRITE_MODE) != 0)
      f.setExecutable((m & EXEC_MODE) != 0)

    val tis =
      if (!compressed) new TarArchiveInputStream(new BufferedInputStream(new FileInputStream(archive)))
      else new TarArchiveInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(archive))))

    try {
      if (!directory.exists()) directory.mkdirs()
      if (!Files.isDirectory(directory.toPath)) throw new IOException(directory.toString + " is not a directory.")

      case class DirectoryMetaData(path: Path, mode: Int, time: Long)
      val directoryRights = ListBuffer[DirectoryMetaData]()

      def filterValue(e: TarArchiveEntry) = filter.map(_(e)).getOrElse(true)

      Iterator.continually(tis.getNextTarEntry).takeWhile(_ != null).filter(filterValue).foreach: e ⇒
        val dest = Paths.get(directory.toString, e.getName)

        //if dest.toFile.getName.contains(".opq") then println("create " + dest)

        if e.isDirectory
        then
          Files.createDirectories(dest)
          directoryRights += DirectoryMetaData(dest, e.getMode, e.getModTime.getTime)
        else
          Files.createDirectories(dest.getParent)

          // has the entry been marked as a symlink in the archive?
          if e.getLinkName.nonEmpty
          then
            val link = Paths.get(e.getLinkName)
            try Files.createSymbolicLink(dest, link)
            catch
              case e: java.nio.file.FileAlreadyExistsException if overwrite =>
                dest.toFile.delete()
                Files.createSymbolicLink(dest, link)

          // file copy from an InputStream does not support COPY_ATTRIBUTES, nor NOFOLLOW_LINKS
          else
            Files.copy(tis, dest, Seq(StandardCopyOption.REPLACE_EXISTING).filter { _ ⇒ overwrite }: _*)
            setMode(dest, e.getMode)

        dest.toFile.setLastModified(e.getModTime.getTime)

      // Set directory right after extraction in case some directory are not writable
      for r ← directoryRights
      do
        setMode(r.path, r.mode)
        r.path.toFile.setLastModified(r.time)

    } finally tis.close()
  }

}
