package it.unibo.spala.iot

import it.unibo.spala.math.Point3D

/**
 * Created by: Roberto Casadei 
 * Created on date: 18/07/15.
 */
trait ThingRegistrationInfo { }

case class BasicRegistrationInfo(id: String, location: Point3D) extends ThingRegistrationInfo
