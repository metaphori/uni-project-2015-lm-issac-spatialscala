package it.unibo.spala.actors

import java.util.TimerTask
import akka.actor.{Actor, ActorRef}
import akka.dispatch.ExecutionContexts
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import it.unibo.spala.math.Point3D
import it.unibo.spala.{CyclicBehavior, Dev}
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.util.Random

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */
class DevActor[S, I](protected var device: Dev[S, (I, Map[ActorRef,S])], var position: Point3D) extends Actor with CyclicBehavior {

  protected val Log = Logger(LoggerFactory.getLogger(self.path.name))

  protected var container: ActorRef = _
  protected var neighbors = Map[ActorRef, Option[S]]()
  protected var input: I = _

  def receive = initialBehavior

  protected def initialBehavior: Receive = {
    case MsgStartWithInputAndRegisterTo(i:I, to) => {
      input = i
      container  = to
      Log.debug("Registering to " + to.path.name)
      container ! MsgRegisterWithPosition(device.name, position)
      Start()
    }
    case MsgStart => {
      Start()
    }
  }

  protected def Start(): Unit = {
    Log.debug("Starting...")
    Thread.sleep(2000)
    DiscoverNeighbors()
    context.become(workingBehavior)
    self ! GoOn
  }

  protected def workingBehavior: Receive = {
    case _:MsgGetState[S] => sender ! MsgReturnState(device.state)
    case m:MsgMove => {
      position = if(m.isRelative) position+m.newpos else m.newpos
      container ! MsgMove(device.name, position, false)
    }
    case GoOn => {
      workingTask()
      scheduleNextElaboration()
      MayMove()
    }
    case MsgGetPosition() => sender ! MsgMove(device.name, position, false)
    case MsgReturnNeighbors(t) => {
      lazy val nones: Stream[Option[S]] = None #:: nones
      var nbs = (t zip nones).toMap
      neighbors = neighbors.filter(kv => t.contains(kv._1))
      neighbors ++= nbs.filter(kv => !neighbors.contains(kv._1))
    }
    case MsgReturnState(nState: S) => {
      neighbors += (sender -> Some(nState))
    }
  }

  protected def workingTask: Unit => Unit = (Unit) => {
    val oldState = device.state
    val map = Map[ActorRef,S](neighbors.filter { case (ref,Some(s)) => true; case _ => false } mapValues (x => x.get) toList : _*)
    device = device.run((input, map))
    val newState = device.state
  }

  private val rand = new Random()
  protected def scheduleNextElaboration() = {
    new java.util.Timer().schedule(new TimerTask {
      override def run(): Unit = {
        SyncWithNeighborValue()
        self ! GoOn
      }
    }, interval.toMillis)

    /* The following piece of code is only for simulating actors going out of scope */
    new java.util.Timer().schedule(new TimerTask {
      override def run(): Unit = {
        DiscoverNeighbors()
      }
    }, (1 second).toMillis)
  }

  implicit val timeout = Timeout(1 second)
  implicit val executionContext = ExecutionContexts.global()

  protected def SyncWithNeighborValue() = {
    neighbors.keys.foreach { n => n ! MsgGetState() }
  }

  protected def DiscoverNeighbors() = {
    container ! MsgGetNeighbors(device.name)
  }

  protected def MayMove() = {
    if (rand.nextInt(100) < 10) {
      val rx, ry = rand.nextInt(20)
      val isRelative = true
      self ! MsgMove(self.path.name, new Point3D(rx - 10, ry - 10, 0), isRelative)
    }
  }

}
