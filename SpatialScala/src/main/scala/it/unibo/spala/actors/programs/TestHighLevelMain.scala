package it.unibo.spala.actors.programs

import akka.actor.ActorRef
import it.unibo.spala.actors.system.{OldSpatialSystem, SpatialSystem}
import it.unibo.spala.config._
import it.unibo.spala.domain.{DBehavior, DeviceBehaviors}
import it.unibo.spala.math.Point3D
import it.unibo.spala.{InputData, InputNames}

import scala.concurrent.duration._

/**
 * Created by: Roberto Casadei 
 * Created on date: 20/07/15.
 */

object TestHighLevel extends App {

  type State = Int
  type Input = Boolean

  //val behavior = DeviceBehaviors.GradientBehavior
  val behavior = DeviceBehaviors.GradientBehavior //map { (i, ctx) => if(i>99) 99 else i } // bounded
  //val behavior = new ConstantBehavior[Int](77)
  //val behavior = (i: Int, ctx: DevContext) => ctx.getNeighborsStates.size
  //val behavior: DevBehavior[Int] = (s: Int, ctx: DevBehaviorContext) => ctx.getComputationRound.toInt

  val settings = Settings[State](
    SystemSettings(
      name = "MyActorSystem",
      executionStrategy = AsyncExecutionStrategy,
      gui = true,
      console = false,
      logLevel = AkkaConfigLogLevel.Info
    ),
    DeviceSettings(
      initialDeviceState = Int.MaxValue,
      inputProvider = (k:Int, name:String, ref:ActorRef, pos:Point3D) => InputData(InputNames.Source1, k==2), // only 50th elem is source
      nameProvider = (k:Int) => "d"+k,
      workInterval = 500 millis,
      mobility = DeviceMobilitySettings(canMove = true, moveStep = 10, moveProbability = 0.6)
    ),
      SpatialSettings(
      proximityThreshold = 80
    ),
    DeviceDistributionSettings(
      n = 100,
      shape = 'grid,
      shapeSettings = GridSettings(nrows = 10, ncols = 10, stepx = 50, stepy = 50, tolerance = 10, offsetx = 100, offsety = 50)
      //shapeSettings = GridSettings(nrows = 1, ncols = 30, stepx = 30, stepy = 50, tolerance = 10, offsetx = 100, offsety = 50)
    ),
    DeploymentSettings(
      kind = LocalDeployment
    )
  )

  new SpatialSystem[State](settings, new DBehavior[State](behavior))
}

