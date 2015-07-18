package it.unibo.spala.iot

/**
 * Created by: Roberto Casadei 
 * Created on date: 10/07/15.
 */

trait ThingsContext[R <: ThingRegistrationInfo,T <: ThingInfo] {

  def RegisterThing(info: R): String

  def GetInfoById(id: String): T

  def GetAll(): Iterable[T]

}
