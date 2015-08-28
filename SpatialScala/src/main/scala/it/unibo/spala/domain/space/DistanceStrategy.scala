package it.unibo.spala.domain.space

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
