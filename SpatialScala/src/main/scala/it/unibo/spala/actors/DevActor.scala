package it.unibo.spala.actors

import java.util.TimerTask
import akka.actor.{Actor, ActorRef}
import akka.dispatch.ExecutionContexts
import akka.pattern.ask
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
  protected var neighbors: List[ActorRef] = List()
  protected var neighborValues = scala.collection.mutable.Map[ActorRef, S]()
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
      self ! GoOn
    }
    case GoOn => workingTask()
    case MsgGetPosition() => sender ! MsgMove(device.name, position, false)
  }

  protected def workingTask: Unit => Unit = (Unit) => {
    val oldState = device.state
    val map = Map[ActorRef,S](neighborValues.toList : _*)
    device = device.run((input, map))
    val newState = device.state

    Log.debug(s"Step: $oldState => $newState\nMap: $map\n\n")

    scheduleNextElaboration()
  }

  private val rand = new Random()
  protected def scheduleNextElaboration() = {
    new java.util.Timer().schedule(new TimerTask {
      override def run(): Unit = {
        SyncWithNeighborValue()

        if (rand.nextInt(100) < 20) {
          val rx, ry = rand.nextInt(100)
          val isRelative = true
          self ! MsgMove(self.path.name, new Point3D(rx - 50, ry - 50, 0), isRelative)
        } else {
          self ! GoOn
        }
      }
    }, interval.toMillis)

    new java.util.Timer().schedule(new TimerTask {
      override def run(): Unit = {
        DiscoverNeighbors()
      }
    }, interval.toMillis)
  }

  implicit val timeout = Timeout(1 second)
  implicit val executionContext = ExecutionContexts.global()

  protected def SyncWithNeighborValue() = {
    neighbors.foreach(n => (n ? MsgGetState()).onComplete(s => s match {
      case scala.util.Success(MsgReturnState(nState: S)) => neighborValues(n) = nState
    })) // dispatch ask + sync neighbor values
  }

  protected def DiscoverNeighbors() = {
    (container ? MsgGetNeighbors(device.name)).onComplete(lst => lst match {
      case scala.util.Success(MsgReturnNeighbors(t)) => {
        neighbors = t
        val torem = neighborValues.keys.filter(k => !neighbors.contains(k))
        torem.foreach(k => neighborValues.remove(k))
        Log.info("Discovering neighbors " + t)
      }
    }) // dispatch ask + update neighbors
  }

}
