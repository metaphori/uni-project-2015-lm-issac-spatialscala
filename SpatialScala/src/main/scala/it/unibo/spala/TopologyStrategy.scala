package it.unibo.spala

import java.util.Random

import it.unibo.spala.math.{Point2D, Point3D}

/**
 * Created by: Roberto Casadei 
 * Created on date: 10/07/15.
 */


/**
 * Defines an abstract spatial container of elements.
 * @tparam E The type of elements
 * @tparam P The type of position
 */
trait Space[E,P] {
  def getLocation(e: E): P

  def getAll(): Iterable[E]
}

trait MutableSpace[E,P] extends Space[E,P] {
  def add(e: E, p: P)

  def remove(e: E)

  def setLocation(e: E, p: P, isRelative: Boolean)
}

trait Basic3DSpace[E] extends MutableSpace[E, Point3D] {
  private var elemPositions: Map[E,Point3D] = Map()

  override def add(e: E, p: Point3D): Unit = {
    elemPositions += (e -> p)
  }

  override def getLocation(e: E): Point3D = elemPositions(e)

  override def getAll(): Iterable[E] = elemPositions.keys

  override def remove(e: E) = elemPositions -= e

  override def setLocation(e: E, p: Point3D, isRel: Boolean) = {
    val newpos = if(isRel) getLocation(e)+p else p
    add(e, newpos)
  }
}

/**
 * Defines an abstract distance strategy for a space.
 * @tparam E The type of elements in the space
 * @tparam P The type of position
 * @tparam D The type of distance
 */
trait DistanceStrategy[E,P,D] {
  this: Space[E,P] =>

  def getDistance(e1: E, e2: E): D

  def getNeighborsWithDistance(e: E) : Iterable[(E,D)]

  def getNeighbors(e: E): Iterable[E]
}

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
