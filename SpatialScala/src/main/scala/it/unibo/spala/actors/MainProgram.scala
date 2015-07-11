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
    idealx <- 200 to 600 by 120;
    idealy <- 60 to 480 by 80;
    rx = rand.nextDouble();
    ry = rand.nextDouble();
    x = idealx + (rx*tolerance - tolerance/2);
    y = idealy + (ry*tolerance - tolerance/2)
  )
    yield Point2D(x,y)

  // CREATE COMPONENTS
  val devs = for(i <- 1 to 10) yield new Device("D"+i, 0, behavior)
  val devsWithPositions = devs zip positions

  val registry = sys.actorOf(Props[SimpleRegistryActor])
  val actorsWithPositions =
    devsWithPositions.map { case (dev,pos) => (sys.actorOf(Props(classOf[DeviceActor], dev, pos) , "Actor"+dev.name), pos) }

  // START THE SYSTEM
  actorsWithPositions.head._1 ! MsgStartWithInputAndRegisterTo(true, registry)
  actorsWithPositions.tail.foreach { case (a,p) => a ! MsgStartWithInputAndRegisterTo(false, registry) }

  val sampler = sys.actorOf(Props[SamplerActor])
  sampler ! MsgStartWithRecipient(registry)

  val gui = sys.actorOf(Props(classOf[GUIActor], 600, 600, actorsWithPositions.toMap))
  gui ! MsgStart

  /*
  trait SlowerCyclicBehavior extends CyclicBehavior {
    override val interval = 5000 milliseconds
  }
  class SlowerDevice(dev: Device) extends DevActor[Int, Boolean](dev) with SlowerCyclicBehavior

  val fdev1: Device = Dev(0, behavior)
  val fdev2: Device = Dev(0, behavior)

  val registry = sys.actorOf(Props[RegistryActor])
  val dev1 = sys.actorOf(Props(classOf[Device], fdev1), "dev1")
  val dev2 = sys.actorOf(Props(classOf[SlowerDevice], fdev2), "dev2")

  Log.info("Actors created")
  Log.info("Starting actors...")

  dev1 ! MsgStartAndRegisterTo(registry)
  dev2 ! MsgStartAndRegisterTo(registry)


  val devs = List(dev1,dev2)
  implicit val timeout = Timeout(5 second)
  implicit val executionContext = ExecutionContexts.global()
  var k = 0
  while(k<10) {
    devs.foreach(d => (d ? MsgGetState[Int]).onComplete(i => Log.info(s"Sampling state = ${i}")))
    k+=1
    Thread.sleep(707)
  }
  */
}