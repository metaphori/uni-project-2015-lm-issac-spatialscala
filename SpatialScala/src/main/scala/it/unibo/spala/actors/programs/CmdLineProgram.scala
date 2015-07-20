package it.unibo.spala.actors.programs

import akka.actor.ActorRef
import it.unibo.spala.actors.SpatialSystem
import it.unibo.spala.config.{SystemSettings, DeviceSettings, Settings}
import it.unibo.spala.math.Point3D
import it.unibo.spala.{DeviceBehaviors, InputData}

import scala.concurrent.duration._

/**
 * Created by: Roberto Casadei 
 * Created on date: 20/07/15.
 */

object CmdLineProgram extends App {

  type State = Int
  type Input = Boolean

  val behavior = DeviceBehaviors.GradientBehavior

  val parser = it.unibo.spala.config.CmdLineParser[State]

  val deviceSettings = DeviceSettings(
    initialDeviceState = 0,
    inputProvider = (k:Int, name:String, ref:ActorRef, pos:Point3D) => InputData("isSrc", k>0 && k%15==0),
    nameProvider = (k:Int) => "d"+k,
    workInterval = 500 millis
  )

  parser.parse(args, Settings(
    deviceSettings = deviceSettings,
    systemSettings = SystemSettings(start = false))
  ) match {
    case Some(settings) =>
      new SpatialSystem[State](settings, behavior)
    case None => parser.showUsage
  }

}
