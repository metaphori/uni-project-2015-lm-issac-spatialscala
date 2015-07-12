package it.unibo.spala.actors

import akka.actor.{Actor, ActorRef}
import it.unibo.spala.CyclicBehavior

/**
 * Created by: Roberto Casadei 
 * Created on date: 09/07/15.
 */
class SamplerActor extends Actor with CyclicBehavior {

  import context.dispatcher // Provides the ExecutionContext

  var obs: List[ActorRef] = List()
  var states: Map[String,Any] = Map()

  def receive = {
    case MsgStartWithRecipient(recipient) => recipient ! MsgGetAll
    case MsgReturnAllAs(all: List[DevInfo]) => {
      obs = all.map(di => di.ref)
      context.become(SamplingBehavior)
      self ! GoOn
    }
  }

  def SamplingBehavior: Receive = {
    case MsgStop => ()
    case MsgReturnState(s) => states += (sender.path.name -> s)
    case GoOn => {
      obs.foreach(d => d ! MsgGetState[Int])
      scheduleNextElaboration()
    }
  }

  protected def scheduleNextElaboration() = context.system.scheduler.scheduleOnce(workInterval){
      states.foreach(kv => print(s"${kv._1}=${kv._2};  "))
      println("\n----------------------------------------------")
      self ! GoOn
    }

}
