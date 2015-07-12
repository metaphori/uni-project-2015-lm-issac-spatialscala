package it.unibo.spala.actors

import akka.actor.{Actor, ActorRef}
import it.unibo.spala.BasicSpace
import it.unibo.spala.math.Point3D

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */

/**
 * Class of actors responsible for managing a space (i.e., a container of devices)
 */
abstract class SpaceManagerActor extends Actor {
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
    case MsgGetPositionOf(who) => sender ! MsgReturnPositionOf(who,GetPositionOf(who))
  }
}

class DevInfo(val id: String, val ref: ActorRef, val position: Option[Point3D]){
}

class SimpleSpaceManagerActor extends SpaceManagerActor with BasicSpace {
  override var actors: Map[String,DevInfo] = Map[String,DevInfo]()

  override protected def RegisterActor(id: String, ref: ActorRef, pos: Option[Point3D] = None) = {
    actors = actors + (id -> new DevInfo(id, ref, pos))
    pos.foreach(p => add(id, p)) // If a position is provided, let's add to the space
  }

  override protected def GetNeighbors(id: String): List[String] = {
    // Utils.neighbors(actors.keys.toList, id, 2).toList
    val nbsWithDist = getNeighborsWithDistance(id)
    getNeighbors(id).filter(nb => nb!=id).toList
  }

  override def UpdateActorPosition(id: String, pos: Point3D, isRelative: Boolean): Unit = {
    setLocation(id, pos, isRelative)
  }

  override protected def GetPositionOf(who: String): Point3D = getLocation(who)
}
