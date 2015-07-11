package it.unibo.spala

/**
 * Created by: Roberto Casadei 
 * Created on date: 10/07/15.
 */
package object math {

  case class Point3D(x: Double, y: Double, z: Double){
    import scala.math.{sqrt, pow}
    def distance(p2: Point3D) = sqrt(pow(p2.x-x, 2) + pow(p2.y-y, 2) +pow(p2.z-z, 2))
  }
  class Point2D(override val x: Double, override val y: Double) extends Point3D(x, y, 0)
  object Point2D { def apply(x: Double, y: Double) = new Point2D(x,y) }
  class Point1D(override val x: Double) extends Point2D(x, 0)
  object Point1D { def apply(x: Double) = new Point1D(x) }

}
