package it.unibo.spala.actors.system

import akka.actor.ActorRef
import it.unibo.spala.actors.DevInfo
import it.unibo.spala.domain.space.BasicSpace
import it.unibo.spala.math.Point3D

/**
 * Created by: Roberto Casadei
 * Created on date: 01/09/15.
 */

class SimpleSpaceManagerActor extends SpaceManagerActor with BasicSpace {
  override var actors: Map[String,DevInfo] = Map[String,DevInfo]()

  override protected def RegisterActor(id: String, ref: ActorRef, pos: Option[Point3D] = None) = {
    Log.info(s"Registering actor $ref with id $id and position $pos")
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
