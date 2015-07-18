package it.unibo.spala.config

import it.unibo.spala.math.Point2D

import scala.util.Random

/**
 * Created by: Roberto Casadei 
 * Created on date: 16/07/15.
 */

object ConfigHelper {

  def GridLocations(gs: GridSettings): List[Point2D] = {
    val rand = new Random
    val tolerance = gs.tolerance * 2 // doubled because can be positive or negative

    val positions = for(
      ncols <- 0 to (gs.ncols - 1);
      nrows <- 0 to (gs.nrows - 1);
      idealx = ncols * gs.stepx + gs.offsetx;
      idealy = nrows * gs.stepy + gs.offsety;
      rx = rand.nextDouble();
      ry = rand.nextDouble();
      x = idealx + (rx*tolerance - tolerance/2);
      y = idealy + (ry*tolerance - tolerance/2)
    )
      yield Point2D(x,y)

    positions.toList
  }

}
