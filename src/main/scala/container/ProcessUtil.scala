package container

import java.io.PrintStream
import org.apache.commons.exec.PumpStreamHandler
import org.apache.commons.exec.ShutdownHookProcessDestroyer

object ProcessUtil {
  val processDestroyer = new ShutdownHookProcessDestroyer

  def execute(cmd: Seq[String], out: PrintStream = System.out, err: PrintStream = System.err, env: Seq[String] = Seq.empty) = {
    val runtime = Runtime.getRuntime
    val process = runtime.synchronized {
      runtime.exec(
        cmd.toArray,
        env.toArray)
    }

    executeProcess(process, out = out, err = err)
  }

  def executeProcess(process: Process, out: PrintStream = System.out, err: PrintStream = System.err) = {
    val pump = new PumpStreamHandler(out, err)

    pump.setProcessOutputStream(process.getInputStream)
    pump.setProcessErrorStream(process.getErrorStream)

    processDestroyer.add(process)
    try {
      pump.start
      try process.waitFor
      catch {
        case e: Throwable ⇒
          def kill(p: ProcessHandle) = p.destroyForcibly()
          process.descendants().forEach(kill)
          kill(process.toHandle)

          throw e
      } finally {
        pump.stop
      }
    } finally processDestroyer.remove(process)
    process.exitValue
  }
}

