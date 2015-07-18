package it.unibo.spala

import it.unibo.spala.Behaviors.DevBehavior

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */

class Dev[S](val name: String, val state: S, val behavior: DevBehavior[S]) extends Serializable {

  def run(ctx: DevContext): Dev[S] = Dev(name, behavior(state, ctx), behavior)

  def apply(ctx: DevContext): Dev[S] = run(ctx)

}

object Dev {
  def apply[S,I](name: String, state: S, behavior: DevBehavior[S]) = new Dev(name, state, behavior)
}