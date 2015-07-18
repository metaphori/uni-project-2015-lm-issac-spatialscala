package it.unibo.spala.space

/**
 * Defines an abstract spatial container of elements.
 * @tparam E The type of elements
 * @tparam P The type of position
 */
trait Space[E,P] {
  def getLocation(e: E): P

  def getAll(): Iterable[E]
}
