package it.unibo.spala.domain.space

import it.unibo.spala.math.Point3D

/**
 * Created by: Roberto Casadei 
 * Created on date: 18/07/15.
 */
trait EuclideanStrategy[E] extends DistanceStrategy[E, Point3D, Double] {
  this: Basic3DSpace[E] =>

  val proximityThreshold: Double = 2
  def proximity(e1: E, e2: E, distance:Double): Boolean = distance <= proximityThreshold

  override def getDistance(e1: E, e2: E): Double = {
    val p1 = getLocation(e1)
    val p2 = getLocation(e2)
    p1.distance(p2)
  }

  override def getNeighbors(e: E): Iterable[E] = getNeighborsWithDistance(e) map (_._1)

  override def getNeighborsWithDistance(e: E): Iterable[(E, Double)] =
    getAll().map(e2 => (e2, getDistance(e, e2))).filter(tp => tp._1!=e && proximity(e, tp._1, tp._2))

}
