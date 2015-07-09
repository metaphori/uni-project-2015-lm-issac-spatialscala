package it.unibo.spala.actors

import akka.actor.{Actor, ActorRef}
import it.unibo.Utils

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */
abstract class RegistryActor extends Actor {
  var actors: Map[String, ActorRef]

  protected def RegisterActor(id: String, actor: ActorRef): Unit

  protected def GetNeighbors(id: String): List[String]

  def receive = {
    case MsgRegister(id) => RegisterActor(id, sender)
    case MsgGetNeighbors(id) => {
      val nbs: List[String] = GetNeighbors(id)
      val nbsRefs: List[ActorRef] = actors.filter(kv => nbs.contains(kv._1)).values.toList
      sender ! MsgReturnNeighbors(nbsRefs)
    }
    case MsgGetAll => sender ! MsgReturnAllAs[List[ActorRef]](actors.values.toList)
  }
}

class SimpleRegistryActor extends RegistryActor {
  override var actors: Map[String,ActorRef] = Map[String,ActorRef]()

  override protected def RegisterActor(id: String, actor: ActorRef) = {
    actors = actors + (id -> actor)
  }

  override protected def GetNeighbors(id: String): List[String] = {
    Utils.neighbors(actors.keys.toList, id, 2).toList
  }

}
