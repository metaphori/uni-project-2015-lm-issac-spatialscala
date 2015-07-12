import akka.actor.{Props, ActorRef, ActorSystem}
import it.unibo.spala.actors._
import it.unibo.spala.math.Point2D
import it.unibo.spala.{Dev, Log}

import scala.util.Random

object MainProgram extends App {
  org.apache.log4j.BasicConfigurator.configure()
  Log.info("Main program started")

  val sys = ActorSystem("MyActorSystem")

  Log.info("Actor system created")

  // CONFIGURATION
  type Device = Dev[Int, (Boolean, Map[ActorRef, Int])]
  type DeviceActor = DevActor[Int, (Boolean, Map[ActorRef, Int])]
  val behavior = (state:Int, input:(Boolean, Map[ActorRef, Int])) => input match {
    case (isSrc, states) => {
      val ss = states.filter { case (ref, s) => s!=(-1) }

      if (isSrc) { 0 }
      else if (!ss.isEmpty) ss.values.min + 1
      else -1
    }
  }

  val rand = new Random
  val tolerance = 80
  val positions = for(
    idealx <- 200 to 600 by 40;
    idealy <- 60 to 480 by 40;
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
    devsWithPositions.map { case (dev,pos) => (dev.name, sys.actorOf(Props(classOf[DeviceActor], dev, pos) , "Actor"+dev.name), pos) }

  // START THE SYSTEM
  var k = 0
  devsInfo.foreach { case (name,a,p) => k+=1; a ! MsgStartWithInputAndRegisterTo((k%20)==19, registry) }

  val sampler = sys.actorOf(Props[SamplerActor])
  sampler ! MsgStartWithRecipient(registry)

  val gui = sys.actorOf(Props(classOf[GUIActor], 600, 600, registry, devsInfo.map({ case (n,a,p) => (n, (a,p)) }).toMap))
  gui ! MsgStart
}