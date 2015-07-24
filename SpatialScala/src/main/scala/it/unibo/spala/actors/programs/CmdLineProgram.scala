package it.unibo.spala.actors.programs

import akka.actor.ActorRef
import it.unibo.spala.{InputData, DeviceBehaviors}
import it.unibo.spala.actors.SpatialSystem
import it.unibo.spala.config.{DeviceSettings, Settings, SystemSettings}
import it.unibo.spala.math.Point3D
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

  parser.parse(args, Settings(
    deviceSettings = null,
    systemSettings = SystemSettings(start = false))
  ) match {
    case Some(settings) =>
      val deviceSettings = DeviceSettings(
        initialDeviceState = 0,
        inputProvider = (k:Int, name:String, ref:ActorRef, pos:Point3D) => InputData("isSrc", k>0 && k%15==0),
        nameProvider = (k:Int) => settings.systemSettings.prefix+k,
        workInterval = 500 millis
      )
      var stgs = settings.copy(deviceSettings = deviceSettings)
      new SpatialSystem[State](stgs, behavior)
    case None => parser.showUsage
  }

}
