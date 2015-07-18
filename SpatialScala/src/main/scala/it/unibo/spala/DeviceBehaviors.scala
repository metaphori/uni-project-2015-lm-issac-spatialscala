package it.unibo.spala

import akka.actor.ActorRef

import scala.math.abs

/**
 * Created by: Roberto Casadei 
 * Created on date: 18/07/15.
 */

package object Behaviors {
  type MooreFunType[S] = S => S
  class DevMooreBehavior[S](val f: MooreFunType[S]) extends MooreFunType[S] with Serializable {
    override def apply(s: S): S = f(s)
  }

  type MealyFunType[S, I] = (S, I) => S
  class DevMealyBehavior[S, I](f: MealyFunType[S, I]) extends MealyFunType[S, I] with Serializable {
    override def apply(s: S, i: I): S = f(s, i)
    def map[T](f2: S => T): (S, I) => T = (s: S, i: I) => f2(f(s,i))
  }

  type MemorylessFunType[S] = () => S
  class MemorylessBehavior[S](f: MemorylessFunType[S]) extends MemorylessFunType[S] with Serializable {
    override def apply(): S = f()
  }

  implicit def fromFunToDevMealyBehavior[S,I](f: MealyFunType[S, I]) = new DevMealyBehavior(f)

  implicit def fromFunToDevMooreBehavior[S](f: MooreFunType[S]) = new DevMooreBehavior(f)

  implicit def fromMealyToMoore[S, I](mealy: DevMealyBehavior[S,I])
                                     (implicit inputProvider: () => I): DevMooreBehavior[S] = {
    mealy(_,inputProvider())
  }

  implicit def fromMemorylessToMoore[S](mless: MemorylessBehavior[S]): DevMooreBehavior[S] = {
    (s: S) => mless()
  }
}

object DeviceBehaviors {
  import it.unibo.spala.Behaviors._

  val GradientBehavior = (s: Int, i: (Boolean,Map[ActorRef,Int])) => i match {
      case (isSrc, states) => {
        if (isSrc) { 0 }
        else if (!states.isEmpty) abs(states.values.min) + 1
        else -1
      }
  }

  class ConstantBehavior[T](const: MemorylessFunType[T]) extends MemorylessBehavior[T](const)

}