package it.unibo.spala

/**
 * Created by: Roberto Casadei 
 * Created on date: 10/07/15.
 */
package object math {

  class Point3D(val x: Double, val y: Double, val z: Double){
    import scala.math.{sqrt, pow}

    def distance(p2: Point3D) = sqrt(pow(p2.x-x, 2) + pow(p2.y-y, 2) +pow(p2.z-z, 2))

    def +(p2: Point3D) = new Point3D(x+p2.x, y+p2.y, z+p2.z)

    override def toString(): String = s"($x;$y;$z)"

    override def equals(other: Any): Boolean = other match {
      case Point3D(x,y,z) => this.x == x && this.y == y && this.z == z
      case _ => false
    }
  }

  object Point3D {
    def unapply(d: Point2D): Option[(Double, Double, Double)] = Some(d.x, d.y, d.z)

    def apply(x: Double, y: Double, z: Double) = new Point3D(x,y,z)
  }

  class Point2D(x: Double, y: Double) extends Point3D(x, y, 0)

  object Point2D {
    def unapply(d: Point2D): Option[(Double, Double)] = Some(d.x, d.y)

    def apply(x: Double, y: Double) = new Point2D(x,y)
  }

  class Point1D(x: Double) extends Point2D(x, 0)

  object Point1D {
    def apply(x: Double) = new Point1D(x)
  }

}
