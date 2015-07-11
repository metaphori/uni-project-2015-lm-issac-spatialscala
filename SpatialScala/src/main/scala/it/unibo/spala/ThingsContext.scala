package it.unibo.spala

import it.unibo.spala.math.Point3D

/**
 * Created by: Roberto Casadei 
 * Created on date: 10/07/15.
 */

trait ThingsContext[R <: ThingRegistrationInfo,T <: ThingInfo] {

  def RegisterThing(info: R): String

  def GetInfoById(id: String): T

  def GetAll(): Iterable[T]

}

trait ThingRegistrationInfo { }

trait ThingInfo { }

case class BasicRegistrationInfo(id: String, location: Point3D)