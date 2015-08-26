package it.unibo.spala.actors

import akka.actor.{Actor, ActorRef}
import it.unibo.spala.InputData

/**
 * Created by: Roberto Casadei 
 * Created on date: 05/08/15.
 */


abstract class InputProviderActor[T](val name: String) extends Actor
  with ActorLifecycleBehavior {

  /* Abstract members */

  var value: T
  def provideNextValue(): T

  /* Key structures */

  val observers: scala.collection.mutable.Set[ActorRef] = scala.collection.mutable.Set()

  /* Utility members */

  private val Log = akka.event.Logging(context.system, this)

  /* Implicits and variables */
  private var round: Long = 0

  /* Reactive behaviors */

  override def setupBehavior = super.setupBehavior.orElse(observersManagementBehavior)

  def observersManagementBehavior: Receive = {
    case MsgAddObserver(o) => observers += o
    case MsgRemoveObserver(o) => observers -= o
  }

  override def DoJob() = {
    this.round += 1
    this.value = provideNextValue()
    notifyObservers()
    ScheduleNextWorkingCycle()
  }

  def notifyObservers() = {
    observers.foreach(o => o ! MsgWithInput(InputData(name,value)))
  }

}
