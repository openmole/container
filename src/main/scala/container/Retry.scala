package container

import squants.time.Time

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
