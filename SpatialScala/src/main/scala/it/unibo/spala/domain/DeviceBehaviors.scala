package it.unibo.spala.domain

import it.unibo.spala.InputNames

/**
 * Created by: Roberto Casadei
 * Created on date: 18/07/15.
 */

/*
class DBehavior[S](b: DevBehavior[S] = (oldS: S, bctx: DevBehaviorContext) => oldS)
  extends DevBehavior[S] with Serializable {
  var behavior: DevBehavior[S] = b
  var s: S = _
  var ctx: DevBehaviorContext = _

  def nbs[T] = ctx.getNeighborsStates[T].values

  def input[T](name: String) = ctx.getInput[T](name)

  override def apply(oldS: S, bctx: DevBehaviorContext): S =  {
    behavior(oldS, bctx)
  }

  def does(f: => S) = {
    behavior = (oldS: S, bCtx: DevBehaviorContext) => {
      s = oldS
      ctx = bCtx
      f
    }
  }

  def map(m: DevBehavior[S]) = (s: S, ctx: DevBehaviorContext) => m.apply(behavior(s, ctx), ctx)
}
*/

class DBehavior[S](val f: MealyFunType[S, DevBehaviorContext]) extends DevBehavior[S] with Serializable {
  override def apply(s: S, i: DevBehaviorContext): S = f(s, i)
  def map(m: DevBehavior[S]) = (s: S, ctx: DevBehaviorContext) => m.apply(f(s, ctx), ctx)
}

trait DevBehaviorContext {
  def getInput[T](name: String): Option[T]

  def getNeighborsStates[S]: Map[String, Option[S]]

  def getComputationRound: Long
}

trait DevContext extends DevBehaviorContext {
  def setInput[T](name: String, value: T): Unit
}

object DeviceBehaviors {
  import math.abs

  val GradientBehavior: DevBehavior[Int] = (s, ctx) => {
    val states = ctx.getNeighborsStates[Int].values.map(o => o.getOrElse(Int.MaxValue))
    val isSrc = ctx.getInput[Boolean]("isSrc").getOrElse(false)
    if (isSrc) 0
    else if (!states.isEmpty) {
      val min = abs(states.min)
      if(min<Int.MaxValue) min+1 else Int.MaxValue
    }
    else Int.MaxValue
  }

  /*
  val GradientBehavior = new DBehavior[Int]{
    does {
      val states = nbs[Int].map(o => o.getOrElse(Int.MaxValue))
      val isSrc = input[Boolean](InputNames.Source1).getOrElse(false)

      if (isSrc) 0
      else if (!states.isEmpty) {
        val min = abs(states.min)
        if(min<Int.MaxValue) min+1 else Int.MaxValue
      }
      else Int.MaxValue
    }
  }

  class ConstantBehavior[T](value: => T) extends DBehavior[T] with Serializable {
    does { value }
  }
  */
}
