package it.unibo.spala.actors

import akka.actor.{Props, ActorRef, ActorSystem}
import it.unibo.spala.Behaviors.DevMealyBehavior
import it.unibo.spala._
import it.unibo.spala.config._
import it.unibo.spala.math.Point3D
import scala.concurrent.duration._

/**
 * Created by: Roberto Casadei 
 * Created on date: 15/07/15.
 */

class MySpaceManager(threshold: Double) extends SimpleSpaceManagerActor {
  override val proximityThreshold: Double = threshold
}

class SpatialSystem[S,I](val name: String,
                         val settings: Settings[S, I],
                         val behavior: DevMealyBehavior[S, I]) {

    val systemSettings = settings.systemSettings
    val devSettings = settings.deviceSettings
    val spaceSettings = settings.spatialSettings
    val devdistributionSettings = settings.devDistributionSettings
    val shapeSettings = devdistributionSettings.shapeSettings
    val deploymentSettings = settings.deploymentSettings

    val sys = ActorSystem(systemSettings.name)

    val positions = devdistributionSettings.shape match {
      case 'grid if shapeSettings.isInstanceOf[GridSettings] =>
        ConfigHelper.GridLocations(shapeSettings.asInstanceOf[GridSettings])
    }

    // CREATE COMPONENTS
    val devs = for(i <- 1 to devdistributionSettings.n)
      yield new Dev[S,I](devSettings.nameProvider(i), devSettings.initialDeviceState.asInstanceOf[S], behavior)
    val devsWithPositions = devs zip positions

    val registry = sys.actorOf(Props(classOf[MySpaceManager], spaceSettings.proximityThreshold), "registry")

    var k = 0
    val devsInfo =
      devsWithPositions.map { case (dev,pos) => {
          val actorProp = Props(classOf[DevActor[S,I]], dev, pos)
          val actorRef = sys.actorOf(actorProp, "Actor" + dev.name)
          (dev.name, actorRef, pos)
        }
      }

    // START THE SYSTEM
    k = 0
    devsInfo.foreach { case (name, a, p) => {
        k += 1;
        val input = devSettings.inputProvider(k, name, a, p)
        a ! MsgStartWithInputAndRegisterTo(input, registry)
      }
    }

    if(systemSettings.console) {
      val sampler = sys.actorOf(Props[SamplerActor])
      sampler ! MsgStartWithRecipient(registry)
    }

    if(systemSettings.gui) {
      val gui = sys.actorOf(Props(classOf[GUIActor], 600, 600, registry))
      import sys.dispatcher
      sys.scheduler.scheduleOnce(1 second) { gui ! MsgStart } // give time to devices to register
    }
}


object TestHighLevel extends App {

  type State = Int
  type Input = (Boolean, Map[ActorRef, State])
  type Device = Dev[State, Input]
  type DeviceActor = DevActor[State, Input]

  val behavior = DeviceBehaviors.GradientBehavior

  val settings = Settings[State,Input](
    SystemSettings(
      name = "MyActorSystem",
      executionStrategy = AsyncExecutionStrategy,
      gui = true,
      console = false),
    DeviceSettings(
      initialDeviceState = 0,
      (k:Int, name:String, ref:ActorRef, pos:Point3D) => k==50, // input provider (only the 50th elem is source)
      (k:Int) => "d"+k // name provider
    ),
    SpatialSettings(
      proximityThreshold = 100
    ),
    DeviceDistributionSettings(
      n = 100,
      shape = 'grid,
      shapeSettings = GridSettings(nrows = 10, ncols = 10, stepx = 50, stepy = 50, tolerance = 10, offsetx = 100, offsety = 50)
    ),
    DeploymentSettings()
  )

  new SpatialSystem[State, Input]("example1", settings, behavior)

}
