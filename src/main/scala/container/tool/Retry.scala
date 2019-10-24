package container.tool

import squants.time.Time

object Retry {

  def retry[T](f: ⇒ T, nbTry: Int, coolDown: Option[Time] = None): T =
    try f
    catch {
      case t: Throwable ⇒
        if (nbTry > 1) {
          coolDown.foreach(c ⇒ Thread.sleep(c.millis))
          retry(f, nbTry - 1)
        } else throw t
    }

  def retry[T](nbTry: Int)(f: ⇒ T): T = retry(f, nbTry)

}
