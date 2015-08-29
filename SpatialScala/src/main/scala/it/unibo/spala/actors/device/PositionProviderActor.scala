package it.unibo.spala.actors.device

import it.unibo.spala.InputNames
import it.unibo.spala.actors.system.SysKB
import it.unibo.spala.math.Point3D

import scala.concurrent.duration._
import scala.util.Random

/**
 * Created by: Roberto Casadei 
 * Created on date: 28/08/15.
 */

// Provide input to the devices
class PositionProviderActor(initial: Point3D)
  extends InputProviderActor[Point3D](InputNames.Position) {

  val rand = new Random()

  override var value = initial

  override def provideNextValue(): Point3D = {
    if (SysKB.MobileDevices && rand.nextDouble() < SysKB.MoveProbability) {
      val rx, ry = rand.nextDouble() * SysKB.MaxMoveStep
      val isRelative = true
      new Point3D(initial.x + rx - SysKB.MaxMoveStep/2, initial.y + ry - SysKB.MaxMoveStep/2, 0)
    }
    else value
  }

  override var workInterval: FiniteDuration = 1 seconds
}