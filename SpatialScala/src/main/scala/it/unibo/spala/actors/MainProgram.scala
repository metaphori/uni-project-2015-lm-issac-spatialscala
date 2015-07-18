import akka.actor.{Props, ActorRef, ActorSystem}
import akka.dispatch.ExecutionContexts
import it.unibo.spala.Behaviors.DevMealyBehavior
import it.unibo.spala.actors._
import it.unibo.spala.math.Point2D
import it.unibo.spala.{Dev, Log}
import scala.concurrent.duration._

import scala.util.Random

object MainProgram extends App {
  org.apache.log4j.BasicConfigurator.configure()
  implicit val executionContext = ExecutionContexts.global()

  Log.info("Main program started")

  val sys = ActorSystem("MyActorSystem")

  Log.info("Actor system created")

  // CONFIGURATION
  type Input = (Boolean, Map[ActorRef, Int])
  type Device = Dev[Int, Input]
  type DeviceActor = DevActor[Int, Input]
  val behavior: DevMealyBehavior[Int, Input] = (state:Int, input:(Boolean, Map[ActorRef, Int])) => input match {
    case (isSrc, states) => {
      if (isSrc) { 0 }
      else if (!states.isEmpty) states.values.min + 1
      else -1
    }
  }

  val rand = new Random
  val tolerance = 80
  val positions = for(
    idealx <- 100 to 600 by 40;
    idealy <- 100 to 480 by 40;
    rx = rand.nextDouble();
    ry = rand.nextDouble();
    x = idealx + (rx*tolerance - tolerance/2);
    y = idealy + (ry*tolerance - tolerance/2)
  )
    yield Point2D(x,y)

  // CREATE COMPONENTS
  val devs = for(i <- 1 to 100) yield new Device("D"+i, 0, behavior)
  val devsWithPositions = devs zip positions

  class MySpaceManager extends SimpleSpaceManagerActor {
    override val proximityThreshold: Double = 50
  }

  val registry = sys.actorOf(Props[MySpaceManager])

  val devsInfo =
    devsWithPositions.map { case (dev,pos) => {
        val actorProp = Props(classOf[DeviceActor], dev, pos)
        val actorRef = sys.actorOf(actorProp, "Actor" + dev.name)
        (dev.name, actorRef, pos)
      }
    }

  // START THE SYSTEM
  var k = 0
  devsInfo.foreach { case (name,a,p) => k+=1; a ! MsgStartWithInputAndRegisterTo((k%20)==19, registry) }

  val sampler = sys.actorOf(Props[SamplerActor])
  sampler ! MsgStartWithRecipient(registry)

  val gui = sys.actorOf(Props(classOf[GUIActor], 600, 600, registry))
  sys.scheduler.scheduleOnce(1 second) { gui ! MsgStart } // give time to devices to register
}