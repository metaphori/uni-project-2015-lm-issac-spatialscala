package it.unibo

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */
package object Utils {

  /**
   * Returns the neighbors up at distance of a given elem.
   *
   * Examples
   *
   * {{{
   * neighbors((1 to 3)++(1 to 3), 1, 1) // => Vector(2,3,1)
   * neighbors((1 to 3)++(1 to 3), 1, 2) // => Vector(2,3,1,2,3)
   * neighbors((1 to 3)++(1 to 3), 1, 3) // => Vector(1,2,3)
   * neighbors((1 to 100), 90, 4) // => Vector(86,87,88,89,90,91,92,93,94)
   * }}}
   *
   * @param s The input sequence
   * @param center
   * @param distance
   * @tparam T
   * @return
   */
  def neighbors[T](s: Seq[T], center: T, distance: Int): Seq[T] = {
    def go(center: T, windows: Iterator[Seq[T]], windowSize: Int, bestDistFromCenter:Int, remember: Seq[T]): Seq[T] = {
      if(windows.hasNext){
        val win = windows.next()
        val index = win.indexOf(center)
        val distFromCenter = if(index<0) Int.MaxValue else math.abs((windowSize/2)-index)
        println("D="+distFromCenter+ " " + index + " " + win)
        index match {
          case -1 => if(remember.isEmpty) go(center, windows, windowSize, bestDistFromCenter, remember) else remember
          case _ if distFromCenter>0 && distFromCenter<bestDistFromCenter => go(center, windows, windowSize, distFromCenter, win)
          case _ if distFromCenter>0 && distFromCenter>bestDistFromCenter => go(center, windows, windowSize, bestDistFromCenter, remember)
          case _ => win
        }

      } else{
        if(!remember.isEmpty) remember.indexOf(center) match {
          case x if x < (windowSize/2) => remember.take(windowSize/2)
          case _ => remember.drop((windowSize/2+1))
        }
        else Seq()
      }
    }
    val winSize = distance*2 + 1
    go(center, s.sliding(winSize), winSize, Int.MaxValue, Seq())
  }

}
