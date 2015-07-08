package it.unibo.spala

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */
case class Dev[S](val state: S, val behavior: DevBehavior[S]) {

  def run: Dev[S] = Dev(behavior(state), behavior)

}
