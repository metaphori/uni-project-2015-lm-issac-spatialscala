package it.unibo.spala.domain

import it.unibo.spala.InputNames

/**
 * Created by: Roberto Casadei
 * Created on date: 18/07/15.
 */

/* This class is not thread-safe */
abstract class DBehavior[S]
  extends DevBehavior[S] with Serializable {
  var behavior: DevBehavior[S] = _
  var s: S = _
  var ctx: DevBehaviorContext = _

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

  def nbs[T] = ctx.getNeighborsStates[T].values

  def nbsValues[T] = ctx.getNeighborsStates[T].values.filter(_.isDefined).map(_.get)

  def nbsValuesOr[T](default: T) = ctx.getNeighborsStates[T].values.map(v => v.getOrElse(default))

  def input[T](name: String) = ctx.getInput[T](name)

  def inputOr[T](name: String, default: T) = ctx.getInput(name).getOrElse(default)

  def map(m: DevBehavior[S]) = (s: S, ctx: DevBehaviorContext) => m.apply(behavior(s, ctx), ctx)
}


trait DevBehaviorContext {
  /* Abstract methods */
  def getInput[T](name: String): Option[T]

  def getNeighborsStates[S]: Map[String, Option[S]]

  def getComputationRound: Long

  /* Concrete methods */

  def nbs[T] = getNeighborsStates[T].values

  def nbsValues[T] = getNeighborsStates[T].values.filter(_.isDefined).map(_.get)

  def nbsValuesOr[T](default: T) = getNeighborsStates[T].values.map(v => v.getOrElse(default))

  def input[T](name: String) = getInput[T](name)

  def inputOr[T](name: String, default: T) = getInput(name).getOrElse(default)
}

trait DevContext extends DevBehaviorContext {
  def setInput[T](name: String, value: T): Unit
}

object DeviceBehaviors {

  class GradientBehavior extends DBehavior[Int] {
    does {
      val states = nbsValuesOr[Int](Int.MaxValue)
      val isSrc = inputOr[Boolean](InputNames.Source1, false)

      if (isSrc) 0
      else if (!states.isEmpty) {
        val min = states.min
        if(min<Int.MaxValue) min+1 else Int.MaxValue
      }
      else Int.MaxValue
    }
  }

  val GradientBehaviorFunction: DevBehavior[Int] = (s: Int, ctx: DevBehaviorContext) => {
    val states = ctx.getNeighborsStates[Int].values.map(_.getOrElse(Int.MaxValue))
    val isSrc = ctx.getInput[Boolean](InputNames.Source1).getOrElse(false)

    if (isSrc) 0
    else if (!states.isEmpty) {
      val min = states.min
      if(min<Int.MaxValue) min+1 else Int.MaxValue
    }
    else Int.MaxValue
  }

  class ConstantBehavior[T](value: T) extends DBehavior[T] with Serializable {
    does { value }
  }
}
