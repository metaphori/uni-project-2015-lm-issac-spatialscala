package it.unibo.spala

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */

case class Dev[S,I](val name: String, val state: S, val behavior: DevMealyBehavior[S,I]) {
  def run(input: I): Dev[S,I] = Dev(name, behavior(state, input), behavior)
}
