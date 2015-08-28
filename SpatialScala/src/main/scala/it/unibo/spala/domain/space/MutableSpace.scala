package it.unibo.spala.domain.space

/**
 * Created by: Roberto Casadei 
 * Created on date: 18/07/15.
 */
trait MutableSpace[E,P] extends Space[E,P] {
  def add(e: E, p: P)

  def remove(e: E)

  def setLocation(e: E, p: P, isRelative: Boolean)
}
