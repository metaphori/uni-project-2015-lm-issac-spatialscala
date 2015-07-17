package it.unibo.spala

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */

class Dev[S,I](val name: String, val state: S, val behavior: DevMealyBehavior[S,I]) extends Serializable {

  def run(input: I): Dev[S,I] = Dev(name, behavior(state, input), behavior)

  def apply(): I => Dev[S,I] = run(_)

}

object Dev {
  def apply[S,I](name: String, state: S, behavior: DevMealyBehavior[S,I]) = new Dev(name, state, behavior)
}