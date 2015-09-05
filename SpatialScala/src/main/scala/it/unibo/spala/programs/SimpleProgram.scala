package it.unibo.spala.programs

import akka.actor.{ActorSystem, Props}
import akka.routing.BroadcastGroup
import com.typesafe.config.ConfigFactory
import it.unibo.spala._
import it.unibo.spala.actors._
import it.unibo.spala.actors.device._
import it.unibo.spala.actors.env.BasicGUIActor
import it.unibo.spala.actors.system.SimpleSpaceManagerActor
import it.unibo.spala.domain._
import it.unibo.spala.math.Point2D

object SimpleProgram extends App {

  // We may need some randomness
  val rand = new scala.util.Random()

  // Type alias for the type of the devices' state
  type S = Int

  // Creation of the actor system
  val sys = ActorSystem("MySystemName", ConfigFactory.defaultApplication())

  // Creation of the devices
  val devs = (1 to 10).map { n =>
    // Create actor for a device with Int state and get its reference
    sys.actorOf(
      Props(classOf[DevActorWithRegistry[S]]),
      "dev"+n)
  }

  // Definition of the behavior function
  val behaviorFun = (s: S, c: DevBehaviorContext) => {
    s+1 // Simply count forward..
  }

  // Then, for each device actor...
  devs.foreach { ref =>
    // Provide its initial state (BEFORE setting the behavior)
    ref ! MsgWithInput(InputData[S](InputNames.InitialState, rand.nextInt(9999)))
    // Provide its behavior
    ref ! MsgWithBehavior(behaviorFun)
    // Create a "GPS sensor" for its simulated position
    val positionInputActor = sys.actorOf(
      Props(classOf[PositionProviderActor],
        Point2D(rand.nextInt(999), rand.nextInt(999))))
    positionInputActor ! MsgAddObserver(ref)
    positionInputActor ! MsgStart
  }

  // Create a broadcast router for the entire group of device actors
  val broadcast = sys.actorOf(
    BroadcastGroup(devs.map(_.path.toString)).props(),
    "groupRouter")

  // Creation of Space Manager, which keeps track of devices and their spatial positions
  val registry = sys.actorOf(
    Props(classOf[SimpleSpaceManagerActor]),
    "registry")

  // Create and start the GUI
  val gui = sys.actorOf(
    Props(classOf[BasicGUIActor], 800, 600),
    "gui")
  gui ! MsgStart

  // Start device actors and register them to the space manager
  broadcast ! MsgStartAndRegisterTo(registry)

  // Make the GUI observe the devices
  broadcast ! MsgAddObserver(gui)
}
