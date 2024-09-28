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

  def archive(directory: File, archive: File, includeDirectoryName: Boolean = false) =
    import java.nio.file.{Files, Paths}
    import java.nio.file.attribute.PosixFilePermission

    val tos = new TarArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(archive)))
    try

      if !Files.isDirectory(directory.toPath) then throw new IOException(directory.toString + " is not a directory.")

      val toArchive = new util.Stack[(File, String)]
      if (!includeDirectoryName) toArchive.push(directory → "") else toArchive.push(directory → directory.getName)

      while !toArchive.isEmpty
      do
        val (source, entryName) = toArchive.pop
        val isSymbolicLink = Files.isSymbolicLink(source.toPath)
        val isDirectory = Files.isDirectory(source.toPath)

        // tar structure distinguishes symlinks
        val e =
          if isDirectory && !isSymbolicLink
          then
            // walk the directory tree to add all its entries to stack
            val stream = Files.newDirectoryStream(source.toPath)
            try
              for f <- stream.asScala
              do
                val newSource = source.toPath.resolve(f.getFileName)
                val newEntryName = entryName + '/' + f.getFileName
                toArchive.push((newSource.toFile, newEntryName))
            finally stream.close()

            // create the actual tar entry for the directory
            new TarArchiveEntry(entryName + '/')
           // tar distinguishes symlinks
          else
            if isSymbolicLink
            then
              val e = new TarArchiveEntry(entryName, TarConstants.LF_SYMLINK)
              e.setLinkName(Files.readSymbolicLink(source.toPath).toString)
              e
            else // plain files
              val e = new TarArchiveEntry(entryName)
              e.setSize(Files.size(source.toPath))
              e

        def posixPermissionsToMode(permissions: Set[PosixFilePermission]): Int =
          var mode = 0

          if permissions.contains(PosixFilePermission.OWNER_READ) then mode |= 0x100 // 0400
          if permissions.contains(PosixFilePermission.OWNER_WRITE) then mode |= 0x080 // 0200
          if permissions.contains(PosixFilePermission.OWNER_EXECUTE) then mode |= 0x040 // 0100

          if permissions.contains(PosixFilePermission.GROUP_READ) then mode |= 0x020 // 0040
          if permissions.contains(PosixFilePermission.GROUP_WRITE) then mode |= 0x010 // 0020
          if permissions.contains(PosixFilePermission.GROUP_EXECUTE) then mode |= 0x008 // 0010

          if permissions.contains(PosixFilePermission.OTHERS_READ) then mode |= 0x004 // 0004
          if permissions.contains(PosixFilePermission.OTHERS_WRITE) then mode |= 0x002 // 0002
          if permissions.contains(PosixFilePermission.OTHERS_EXECUTE) then mode |= 0x001 // 0001

          mode

        // complete current entry by fixing its modes and writing it to the archive
        if source != directory
        then
          if !isSymbolicLink
          then
            val mode = posixPermissionsToMode(Files.getPosixFilePermissions(source.toPath).asScala.toSet)
            e.setMode(mode)

          tos.putArchiveEntry(e)

          if Files.isRegularFile(source.toPath, LinkOption.NOFOLLOW_LINKS)
          then try Files.copy(source.toPath, tos)

          finally tos.closeArchiveEntry()

    finally tos.close()



  def extract(archive: File, directory: File, overwrite: Boolean = true, compressed: Boolean = false, filter: Option[TarArchiveEntry => Boolean] = None) =
    import java.nio.file.{Files, Paths}
    import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream}
    import java.io.FileInputStream

    /** set mode from an integer as retrieved from a Tar archive */
    def setMode(file: Path, mode: Int) =
      import java.nio.file.attribute.*
      import scala.jdk.CollectionConverters._

      val permissionSet = scala.collection.mutable.Set[PosixFilePermission]()

      if (mode & 0x100) != 0 then permissionSet += PosixFilePermission.OWNER_READ
      if (mode & 0x080) != 0 then permissionSet += PosixFilePermission.OWNER_WRITE
      if (mode & 0x040) != 0 then permissionSet += PosixFilePermission.OWNER_EXECUTE

      if (mode & 0x020) != 0 then permissionSet += PosixFilePermission.GROUP_READ
      if (mode & 0x010) != 0 then permissionSet += PosixFilePermission.GROUP_WRITE
      if (mode & 0x008) != 0 then permissionSet += PosixFilePermission.GROUP_EXECUTE

      if (mode & 0x004) != 0 then permissionSet += PosixFilePermission.OTHERS_READ
      if (mode & 0x002) != 0 then permissionSet += PosixFilePermission.OTHERS_WRITE
      if (mode & 0x001) != 0 then permissionSet += PosixFilePermission.OTHERS_EXECUTE

      val f = file.toRealPath().toFile

      // Set the permissions on the extracted file or directory
      Files.setPosixFilePermissions(f.toPath, permissionSet.asJava)

    val tis =
      if !compressed
      then new TarArchiveInputStream(new BufferedInputStream(new FileInputStream(archive)))
      else new TarArchiveInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(archive))))

    try
      if !directory.exists() then directory.mkdirs()
      if !Files.isDirectory(directory.toPath) then throw new IOException(directory.toString + " is not a directory.")

      case class DirectoryMetaData(path: Path, mode: Int, time: Long)
      val directoryData = ListBuffer[DirectoryMetaData]()

      case class LinkData(dest: Path, linkName: String, hard: Boolean)
      val linkData = ListBuffer[LinkData]()

      def filterValue(e: TarArchiveEntry) = filter.map(_(e)).getOrElse(true)

      Iterator.continually(tis.getNextTarEntry).takeWhile(_ != null).filter(filterValue).foreach: e ⇒
        val dest = Paths.get(directory.toString, e.getName)

        if e.isDirectory
        then
          Files.createDirectories(dest)
          directoryData += DirectoryMetaData(dest, e.getMode, e.getModTime.getTime)
        else
          Files.createDirectories(dest.getParent)

          // has the entry been marked as a symlink in the archive?
          if e.getLinkName.nonEmpty
          then linkData += LinkData(dest, e.getLinkName, e.isLink)
            // file copy from an InputStream does not support COPY_ATTRIBUTES, nor NOFOLLOW_LINKS
          else
            Files.copy(tis, dest, Seq(StandardCopyOption.REPLACE_EXISTING).filter { _ ⇒ overwrite }: _*)
            setMode(dest, e.getMode)

        dest.toFile.setLastModified(e.getModTime.getTime)


      // Process links
      for l <- linkData
      do
        if !l.hard
        then
          val link = Paths.get(l.linkName)
          try Files.createSymbolicLink(l.dest, link)
          catch
            case e: java.nio.file.FileAlreadyExistsException if overwrite =>
              l.dest.toFile.delete()
              Files.createSymbolicLink(l.dest, link)
        else
          val link = Paths.get(directory.toString, l.linkName)
          try Files.createLink(l.dest, link)
          catch
            case e: java.nio.file.FileAlreadyExistsException if overwrite =>
              l.dest.toFile.delete()
              Files.createLink(l.dest, link)

      // Set directory right after extraction in case some directory are not writable
      for r ← directoryData
      do
        setMode(r.path, r.mode)
        r.path.toFile.setLastModified(r.time)

    finally tis.close()

}
