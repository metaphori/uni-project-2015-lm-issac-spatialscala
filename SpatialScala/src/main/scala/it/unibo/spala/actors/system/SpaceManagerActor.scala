package it.unibo.spala.actors.system

import akka.actor.{Actor, ActorRef}
import it.unibo.spala.actors._
import it.unibo.spala.math.Point3D
import it.unibo.spala.domain.space.BasicSpace

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */

/**
 * Class of actors responsible for managing a space (i.e., a container of devices)
 */
abstract class SpaceManagerActor extends Actor {
  protected val Log = akka.event.Logging(context.system, this)

  var actors: Map[String, DevInfo]

  protected def RegisterActor(id: String, actor: ActorRef, pos: Option[Point3D] = None): Unit

  protected def UpdateActorPosition(id: String, pos: Point3D, isRelative: Boolean): Unit

  protected def GetNeighbors(id: String): List[String]

  protected def GetPositionOf(who: String): Point3D

  def receive = {
    case MsgRegisterWithPosition(id, pos) => RegisterActor(id, sender, Some(pos))
    case MsgMove(id, pos, isRelative) => UpdateActorPosition(id, pos, isRelative)
    case MsgGetNeighbors(id) => {
      val nbs: List[String] = GetNeighbors(id)
      val nbsRefs: List[ActorRef] = actors.filter(kv => nbs.contains(kv._1)).map(info => info._2.ref).toList
      sender ! MsgReturnNeighbors(nbsRefs)
    }
    case MsgGetAll => sender ! MsgReturnAllAs[List[DevInfo]](actors.values.toList)
    case MsgGetAllWithInfo => sender ! MsgReturnAllAs[Map[String, DevInfo]](actors)
    case MsgGetPositionOf(who) => sender ! MsgReturnPositionOf(who,GetPositionOf(who))
  }
}


