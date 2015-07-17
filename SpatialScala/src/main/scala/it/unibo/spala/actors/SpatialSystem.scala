package it.unibo.spala.actors

import akka.actor.{Props, ActorRef, ActorSystem}
import it.unibo.spala._
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
                         val settings: Settings,
                         val behavior: DevMealyBehavior[S, I]) {

    val systemSettings = settings.systemSettings

    val sys = ActorSystem(systemSettings.name)

    val deploymentSettings = settings.deploymentSettings
    val devdistributionSettings = settings.devDistributionSettings
    val shapeSettings = devdistributionSettings.shapeSettings

    val positions = devdistributionSettings.shape match {
      case 'grid if shapeSettings.isInstanceOf[GridSettings] =>
        ConfigHelper.GridLocations(shapeSettings.asInstanceOf[GridSettings])
    }

    // CREATE COMPONENTS
    val devs = for(i <- 1 to devdistributionSettings.n)
      yield new Dev[S,I]("D"+i, systemSettings.initialDeviceState.asInstanceOf[S], behavior)
    val devsWithPositions = devs zip positions

    val registry = sys.actorOf(Props(classOf[MySpaceManager], systemSettings.proximityThreshold), "registry")
    /* Note, you need to pass "this" to make it work with the nested class.
     * Remember: the nested class belongs to the instance of the containing class!
     */

    val devsInfo =
      devsWithPositions.map { case (dev,pos) => {
          val actorProp = Props(classOf[DevActor[S,I]], dev, pos)
          val actorRef = sys.actorOf(actorProp, "Actor" + dev.name)
          (dev.name, actorRef, pos)
        }
      }

    // START THE SYSTEM
    var k = 0
    devsInfo.foreach { case (name, a, p) => {
        k += 1;
        val input = systemSettings.inputProvider(k, name, a, p)
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

  val behavior: DevMealyBehavior[Int, (Boolean,Map[ActorRef,Int])] = (olds: Int, input: (Boolean, Map[ActorRef, Int])) => input match {
    case (isSrc, states) => {
      if (isSrc) { 0 }
      else if (!states.isEmpty) states.values.min + 1
      else -1
    }
  }

  val settings = Settings(
    SystemSettings(
      name = "MyActorSystem",
      initialDeviceState = 0,
      (k:Int, name:String, ref:ActorRef, pos:Point3D) => k==50, // only the 50th elem is source
      proximityThreshold = 50,
      gui = true,
      console = false),
    DeploymentSettings(),
    DeviceDistributionSettings(
      n = 100,
      shape = 'grid,
      shapeSettings = GridSettings(nrows = 10, ncols = 10, stepx = 50, stepy = 50, tolerance = 10, offsetx = 100, offsety = 50)
    )
  )

  new SpatialSystem[State, Input]("example1", settings, behavior)

}
