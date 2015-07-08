import akka.actor.{ActorSystem, Props}
import akka.dispatch.ExecutionContexts
import akka.pattern.ask
import akka.util.Timeout
import it.unibo.spala.actors._
import it.unibo.spala.{Dev,Log}

import scala.concurrent.duration._

object MainProgram extends App {
  org.apache.log4j.BasicConfigurator.configure()
  Log.info("Main program started")

  val sys = ActorSystem("MyActorSystem")

  Log.info("Actor system created")

  type Device = DevActor[Int]
  trait SlowerCyclicBehavior extends CyclicBehavior {
    override val interval = 5000 milliseconds
  }
  class SlowerDevice(dev: Dev[Int]) extends DevActor[Int](dev) with SlowerCyclicBehavior

  val fdev1: Dev[Int] = Dev(0, i => i+1)
  val fdev2: Dev[Int] = Dev(0, i => i-1)

  val dev1 = sys.actorOf(Props(classOf[Device], fdev1), "dev1")
  val dev2 = sys.actorOf(Props(classOf[SlowerDevice], fdev2), "dev2")

  Log.info("Actors created")
  Log.info("Starting actors...")

  dev1 ! MsgStart
  dev2 ! MsgStart

  Thread.sleep(2000)

  implicit val timeout = Timeout(5 second)
  implicit val executionContext = ExecutionContexts.global()
  var s = dev1 ? MsgGetState[Int]
  s.onComplete(i => Log.info(s"Sampling state = ${i}" ))

}