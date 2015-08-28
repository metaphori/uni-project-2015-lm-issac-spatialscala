package it.unibo.spala.actors.programs

import akka.actor.ActorRef
import it.unibo.spala.actors.system.SpatialSystem
import it.unibo.spala.config.{Settings, SystemSettings}
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

  parser.parse(args, Settings[State](
    systemSettings = SystemSettings(start = false))
  ) match {
    case Some(settings) =>
      var stgs: Settings[State] = settings.copy(deviceSettings = settings.deviceSettings.copy(
        initialDeviceState = 0,
        inputProvider = (k:Int, name:String, ref:ActorRef, pos:Point3D) => InputData("isSrc", k>0 && k%50==0),
        nameProvider = (k:Int) => settings.systemSettings.prefix+k,
        workInterval = 500 millis
      ))
      new SpatialSystem[State](stgs, behavior)
    case None => parser.showUsage
  }

}
