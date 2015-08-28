package it.unibo.spala.actors.system

import scala.concurrent.duration._

/**
 * Created by: Roberto Casadei 
 * Created on date: 20/07/15.
 */

object SysKB {

  var WorkInterval: FiniteDuration = 1 second

  var StatePropagationInterval: FiniteDuration = 1 second

  var NbsDiscoveryInterval: FiniteDuration = 500 millis

  var MobileDevices = true

  var MoveProbability = 0.2

  var MoveInterval = 100 milliseconds

  var MaxMoveStep = 20.0

}
