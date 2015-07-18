package it.unibo.spala

/**
 * Created by: Roberto Casadei
 * Created on date: 18/07/15.
 */

package object Behaviors {
  /*
  trait DevBehavior[S] extends MealyFunType[S, DevContext] {
    def map[T](f: S => T): (S, DevContext) => T = (s:S, ctx:DevContext) => f(this.apply(s, ctx))
  }*/

  type DevBehavior[S] = MealyFunType[S, DevContext]

  type MooreFunType[S] = S => S

  type MealyFunType[S, I] = (S, I) => S

  type MemorylessFunType[S] = () => S

  /*
  implicit def fromMealyFunToBehavior[S, I](f: MealyFunType[S, I]): MealyBehavior[S, I] = new MealyBehavior(f)
  class MealyBehavior[S, I](val f: MealyFunType[S, I]) extends ((S, I) => S) {
    override def apply(s: S, i: I): S = f(s, i)
    def map(m: MealyBehavior[S,I]) = (s: S, i: I) => m.apply(f(s,i),i)
  }
  */

  class DBehavior[S](val f: MealyFunType[S, DevContext]) extends DevBehavior[S] with Serializable {
    override def apply(s: S, i: DevContext): S = f(s, i)
    def map(m: DevBehavior[S]) = (s: S, ctx: DevContext) => m.apply(f(s, ctx), ctx)
  }

}

trait DevContext {
  def setInput[T](name: String, value: T): Unit

  def getInput[T](name: String): Option[T]

  def getNeighborsStates[S]: Map[String, Option[S]]
}

object DeviceBehaviors {
  import it.unibo.spala.Behaviors._

  /*
  val GradientBehavior: DevBehavior[Int] = (s, ctx) => {
    val states = ctx.getNeighborsStates[Int].values.map(o => o.getOrElse(-1))
    val isSrc = ctx.getInput[Boolean]("isSrc").getOrElse(false)
    if (isSrc) 0
    else if (!states.isEmpty) abs(states.min) + 1
    else -1
  }
  */

  val GradientBehavior = new DBehavior[Int](
    (s, ctx) => {
      val states = ctx.getNeighborsStates[Int].values.map(o => o.getOrElse(-1))
      val isSrc = ctx.getInput[Boolean]("isSrc").getOrElse(false)
      if (isSrc) 0
      else if (!states.isEmpty) scala.math.abs(states.min) + 1
      else -1
    })

  class ConstantBehavior[T](value: => T) extends DBehavior[T]( (t,ctx) => value ) with Serializable

}