package it.unibo.spala.actors

import akka.actor.Actor
import it.unibo.spala.InputData
import it.unibo.spala.actors.patterns.{ActorLifecycleBehavior, ObservableActor}


abstract class InputProviderActor[T](val name: String) extends Actor
  with ActorLifecycleBehavior with ObservableActor {

  /* Abstract members */

  var value: T
  def provideNextValue(): T
  
  /* Utility members */

  private val Log = akka.event.Logging(context.system, this)

  /* Reactive behaviors */

  override def setupBehavior = super.setupBehavior.orElse(observersManagementBehavior)

  override def DoJob() = {
    this.value = provideNextValue()
    NotifyObservers()
    ScheduleNextWorkingCycle()
  }
  
  override def CurrentStateMessage: Any = {
    MsgWithInput(InputData(name,value))
  }
}
