package it.unibo.spala

import scala.math.abs

/**
 * Created by: Roberto Casadei 
 * Created on date: 18/07/15.
 */
object DeviceBehaviors {

  object GradientBehavior extends DevMealyBehavior[Int, (Boolean,Map[_,Int])] with Serializable {
    override def apply(oldState: Int, input: (Boolean, Map[_, Int])): Int = input match {
      case (isSrc, states) => {
        if (isSrc) { 0 }
        else if (!states.isEmpty) abs(states.values.min) + 1
        else -1
      }
    }
  }

  class ConstantBehavior[T](const: => T) extends MemorylessBehavior[T] {
    lazy val value = const
    override def apply(): T = value
  }

}
