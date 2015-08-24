package it.unibo.spala.actors

import akka.actor.{Actor, ActorRef}
import it.unibo.spala.InputData

import scala.concurrent.duration.FiniteDuration

/**
 * Created by: Roberto Casadei 
 * Created on date: 05/08/15.
 */


abstract class InputProviderActor[T](val name: String) extends Actor {

  /* Abstract members */

  var value: T
  def provideNextValue(): T
  val workInterval: FiniteDuration

  /* Key structures */

  val observers: scala.collection.mutable.Set[ActorRef] = scala.collection.mutable.Set()

  /* Utility members */

  private val Log = akka.event.Logging(context.system, this)

  /* Implicits and variables */

  import context.dispatcher // Provides the ExecutionContext
  private var round: Long = 0

  /* Reactive behaviors */

  def receive = initialBehavior.orElse(observersManagementBehavior)

  protected def initialBehavior: Receive = {
    case MsgStart => Start()
  }

  def Start() = {
    context.become(workingBehavior.orElse(observersManagementBehavior))
    self ! GoOn
  }

  protected def workingBehavior: Receive = {
    case GoOn => {
      this.round += 1
      this.value = provideNextValue()
      notifyObservers()
      scheduleNextElaboration()
    }
  }

  def observersManagementBehavior: Receive = {
    case MsgAddObserver(o) => observers += o
    case MsgRemoveObserver(o) => observers -= o
  }

  protected def scheduleNextElaboration() = {
    context.system.scheduler.scheduleOnce(workInterval) {
      self ! GoOn
    }
  }

  def notifyObservers() = {
    observers.foreach(o => o ! MsgWithInput(InputData(name,value)))
  }

}
