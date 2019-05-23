package container

import better.files._
import java.io.IOException
import container.Status._
import scala.sys.process._
import container.ImageBuilder._
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}

object Extractor {
    
    /** Split a filepath into a path and a filename
     */
    def getPathAndFileName(filepath: String) = {
        val files = filepath.split('/')
        val filename = files(files.length - 1)
        val path = filepath.substring(0, filepath.length - filename.length)
        (path, filename)
    }

    /** Detect whether a file is an archive or not
      * using its extension (quick and dirty way, yes).
      */
    def isAnArchive(filename: String) = {
        if (
            filename.endsWith(".tar") ||
            filename.endsWith(".gz") ||
            filename.endsWith(".tgz") ||
            filename.endsWith(".zip") ||
            filename.endsWith(".bz2") ||
            filename.endsWith(".xz") ||
            filename.endsWith(".7z") ||
            filename.endsWith(".rar")
        )
            true
        else
            false
    }
    
    /** Create a directory path corresponding to an archive
      * by removing the extension from the archive name
      * and keeping the same path dsd.
      *
      * This function can be modified later to for example include
      * a random id in the directory name, if you want to make it
      * unique.
      */
    def getDirectoryPath(path: String, fileName: String) = {
        val directoryPath = if (path.isEmpty) "./" else path
        val directoryName = fileName.split('.')(0)
        directoryPath + directoryName
    }
    
    /** Make sure that the path ends with '/'
      */
    def completeDirectoryPath(path: String) = {
        if (path.endsWith("/")) path
        else path + "/"
    }
    
    def createDirectory(directoryPath: String): Status = {
        val directory = File(directoryPath)
        if(directory.exists && !directory.isDirectory)
            return DIRECTORY_FILE_COLLISION.withArgument(directoryPath)
        
        try {
            directory.createDirectory()
            return OK
        }
        catch {
            case se: SecurityException  => return SECURITY_ERROR.withArgument(se) 
            case ioe: IOException       => return IO_ERROR.withArgument(ioe)  
            case e: Exception           => return UNKNOWN_ERROR.withArgument(e)
        }
    }
    
    def executeCommand(cmd: String): (Int, String, String) = {
        val stdout = new StringBuilder
        val stderr = new StringBuilder
        val status = cmd !  ProcessLogger(stdout append _, stderr append _)
        if (status != 0) throw CommandExecutionError(status, stdout.toString(),stderr.toString())
        (status, stdout.toString(),stderr.toString())
    }
    
    def extractLines(file: File) = {
        val test = file.contentAsString
        val source = scala.io.Source.fromFile(file.pathAsString)
        val lines = try source.mkString finally source.close()
        //lines

        test
    }
    /*
    def oldExtractArchive(archivePath: String, extractionDirectory: String) = {
        val cmd = "tar -xvf " + archivePath + " -C " + extractionDirectory
        val (status, stdout, stderr) = executeCommand(cmd)
        if (status != 0) UNKNOWN_ERROR
        else OK
    }
*/

  import org.apache.commons.compress.archivers._

  import org.apache.commons.compress.archivers.tar.TarArchiveEntry
    import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
    import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
    import org.apache.commons.compress.utils.IOUtils
    import java.io.File
    import java.io.FileInputStream
    import java.io.FileOutputStream
    import java.io.IOException

 // val test = TarArchiveOutputStream


    def extractArchive(archivePath: String, extractionDirectory: String, verbose: Boolean = true): Unit = {
        val cmd =
            if (verbose) "tar -xvf " + archivePath + " -C " + extractionDirectory
            else "tar -xf " + archivePath + " -C " + extractionDirectory
        executeCommand(cmd)
    }

    def compressFile(dirPath: String, archiveName: String, verbose: Boolean = true): Unit = {
     //   val cmd =
        /*    if (verbose) */
     //   println(s"find $dirPath -printf \'%P\\n\' | tar -cvf $dirPath.tar --no-recursion -C $dirPath/ -T -")
     //   println(File(archiveName).toJava.listFiles().map(_.getName).toString)// .toJava.listFiles().map(_.getName).toList)
        /*Seq("find",  dirPath, "-printf", "\"%P\\n\"") #| */ //Process("cd", File(dirPath).toJava) #| Seq("tar", "-cvf", archiveName +".tar", File(dirPath).toJava.listFiles().toString) !!
      //  Seq(s"find $dirPath", "-printf", "\"\%P\\n\"") #| Seq(s"tar -cvf $archiveName.tar --no-recursion -C $dirPath -T -") !!
            //"tar -cvf " + archiveName + ".tar -C " + dirPath + " ."
      //      else         s"find $dirPath -printf \"%P\\n\" | tar -cf $archiveName.tar --no-recursion -C $dirPath -T -"/

        val cmd = "tar -cf " + archiveName + ".tar -C " + dirPath + " ."
        println(cmd)

        executeCommand(cmd)
    }



  /*
    def extractArchiveRSync(archivePath: String, extractionDirectory: String) = {
        val cmd = "rsync -aHSx --no-devices --no-specials " + archivePath + " " + extractionDirectory
        val (status, result) = executeCommand(cmd)
        if (status.isBad)
            status
        else
            OK
    }
*/
}

