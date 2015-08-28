package it.unibo.spala.domain.space

import java.util.Random

import it.unibo.spala.math.Point2D

/**
 * Created by: Roberto Casadei 
 * Created on date: 18/07/15.
 */
trait BasicSpace extends Basic3DSpace[String] with EuclideanStrategy[String] {

  def addManyIn2DGrid(cols: Int, rows: Int,
                      colWidth: Double, rowHeight: Double,
                      tolerance: Double = 0): Unit = {
    val randGen = new Random()
    var k = 0
    for (c <- 0 to cols;
         r <- 0 to rows;
         deltac = randGen.nextDouble() * tolerance - (tolerance / 2);
         deltar = randGen.nextDouble() * tolerance - (tolerance / 2);
         (x, y) = (c * colWidth + deltac, r * rowHeight + deltar)) {
      add("d" + k, Point2D(x, y))
      k += 1
    }
  }

}
