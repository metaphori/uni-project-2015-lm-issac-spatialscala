package it.unibo.spala.actors.patterns

import akka.actor.Actor._
import akka.actor.ActorRef
import it.unibo.spala.actors.{MsgAddObserver, MsgRemoveObserver}

/**
 * Created by: Roberto Casadei 
 * Created on date: 05/08/15.
 */

trait ObservableActor {
  /* Abstract members */

  def CurrentStateMessage: Any

  /* Key structures */

  val observers: scala.collection.mutable.Set[ActorRef] = scala.collection.mutable.Set()

  /* Behavior */

  def observersManagementBehavior: Receive = {
    case MsgAddObserver(o) => observers += o
    case MsgRemoveObserver(o) => observers -= o
  }

  def NotifyObservers() = {
    observers.foreach(o => o ! CurrentStateMessage)
  }
}
