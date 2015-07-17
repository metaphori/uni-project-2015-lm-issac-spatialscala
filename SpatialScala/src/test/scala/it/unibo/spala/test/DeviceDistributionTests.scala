package it.unibo.spala.test

/**
 * Created by: Roberto Casadei 
 * Created on date: 16/07/15.
 */

import it.unibo.spala.math.{Point2D}
import it.unibo.spala.{ConfigHelper, GridSettings}
import org.scalatest._

class DeviceDistributionTests extends FlatSpec with Matchers {

  "ConfigHelper" should "allow me to build a grid of positions" in {
    val gridSettings = GridSettings(nrows = 3, ncols = 2, stepx = 10, stepy = 20, tolerance = 0, offsetx = 7, offsety = -3)

    val actualPositions = ConfigHelper.GridLocations(gridSettings)

    object p { def apply(x:Double, y:Double) = Point2D(x,y) }
    val expectedPositions = List(
      p(0+7, 0-3), p(0+7, 20-3), p(0+7, 40-3), /* 1st col */
      p(10+7,0-3), p(10+7,20-3), p(10+7,40-3)  /* 2nd col */
    )

    actualPositions shouldEqual expectedPositions
  }

  "ConfigHelper" should "allow me to build a grid of fuzzy positions" in {
    import math.abs

    val tolerance = 5
    val fuzzyGridSettings = GridSettings(nrows = 3, ncols = 2, stepx = 10, stepy = 20, tolerance = tolerance)
    val idealGridSettings = GridSettings(nrows = 3, ncols = 2, stepx = 10, stepy = 20, tolerance = 0)

    val fuzzyPositions = ConfigHelper.GridLocations(fuzzyGridSettings)
    val idealPositions = ConfigHelper.GridLocations(idealGridSettings)

    object p { def apply(x:Double, y:Double) = Point2D(x,y) }
    val expectedIdealPositions = List(
      p(0 , 0), p( 0,20), p( 0,40), /* 1st col */
      p(10, 0), p(10,20), p(10,40)  /* 2nd col */
    )

    idealPositions shouldEqual expectedIdealPositions
    fuzzyPositions.length shouldEqual idealPositions.length
    withClue(fuzzyPositions) {
      (fuzzyPositions zip idealPositions) flatMap {
        case (Point2D(x1,y1), Point2D(x2,y2)) => List(abs(x1-x2), abs(y1-y2))
      } forall (_ <= tolerance) shouldBe true
    }
  }

}
