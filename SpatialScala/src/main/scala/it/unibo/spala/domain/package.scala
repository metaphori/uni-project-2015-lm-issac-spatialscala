package it.unibo.spala

import it.unibo.spala.domain._

/**
 * Created by: Roberto Casadei 
 * Created on date: 28/08/15.
 */
package object domain {
  type DevBehavior[S] = MealyFunType[S, DevBehaviorContext]

  type MooreFunType[S] = S => S

  type MealyFunType[S, I] = (S, I) => S

  type MemorylessFunType[S] = () => S
}