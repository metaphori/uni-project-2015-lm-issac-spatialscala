package it.unibo.spala.domain.space

import it.unibo.spala.math.Point3D

/**
 * Created by: Roberto Casadei 
 * Created on date: 18/07/15.
 */
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
