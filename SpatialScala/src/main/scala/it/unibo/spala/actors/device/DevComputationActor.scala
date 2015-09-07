package it.unibo.spala.actors.device

import akka.actor.Actor
import it.unibo.spala.actors._
import it.unibo.spala.actors.patterns.ActorLifecycleBehavior
import it.unibo.spala.domain.{DevBehaviorContext, DevBehavior}

import scala.concurrent.duration.FiniteDuration

/**
 * Created by: Roberto Casadei
 * Created on date: 01/09/15.
 */
class DevComputationActor[S](computation: DevBehavior[S],
                             override var workInterval: FiniteDuration,
                             initialState: Option[S])
  extends Actor
  with ActorLifecycleBehavior
  with DevBehaviorContext {

  protected val Log = akka.event.Logging(context.system, self.path.toString)
  var neighbors = Map[String, Option[S]]()
  var inputs: Map[String, Any] = Map()

  var state: Option[S] = initialState

  override def workingBehavior: Receive = {
    case MsgUpdateNeighbors(nbs: Map[String,Option[S]]) => neighbors = nbs
    case MsgUpdateInputs(inps: Map[String,Any]) => inputs = inps
    case MsgGetState => sender() ! MsgReturnState(this.state)
  }

  override def DoJob() = {
    this.state = this.state.map(olds => computation(olds, this))
    this.state.foreach(s => {
      context.parent ! MsgReturnValue(s)
    })
    ScheduleNextWorkingCycle()
  }

  override def getNeighborsStates[S]: Map[String, Option[S]] = neighbors.asInstanceOf[Map[String, Option[S]]]

  override def getInput[T](name: String): Option[T] =
    inputs.get(name) match { case Some(t:T) => Some(t); case _ => None }

  override def getComputationRound: Long = 1
}
