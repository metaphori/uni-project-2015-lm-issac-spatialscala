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

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */
class DevActor[S, I](protected var device: Dev[S, (I, Map[ActorRef,S])], val position: Point3D) extends Actor with CyclicBehavior {

  protected val Log = Logger(LoggerFactory.getLogger(self.path.name))

  protected var registry: ActorRef = _
  protected var neighbors: List[ActorRef] = List()
  protected var neighborValues = scala.collection.mutable.Map[ActorRef, S]()
  protected var input: I = _

  def receive = initialBehavior

  protected def initialBehavior: Receive = {
    case MsgStartWithInputAndRegisterTo(i:I, to) => {
      input = i
      registry  = to
      Log.debug("Registering to " + to.path.name)
      registry ! MsgRegisterWithPosition(device.name, position)
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
    case _:MsgGetState[S] => { sender ! MsgReturnState(device.state) }
    case GoOn => workingTask()
  }

  protected def workingTask: Unit => Unit = (Unit) => {
    val oldState = device.state
    val map = Map[ActorRef,S](neighborValues.toList : _*)
    device = device.run((input, map))
    val newState = device.state
    //Log.debug(s"Step: $oldState => $newState")
    scheduleNextElaboration()
  }

  protected def scheduleNextElaboration() = { new java.util.Timer().schedule(new TimerTask {
    override def run(): Unit = {
      SyncWithNeighborValue()
      self ! GoOn
    }
  }, interval.toMillis) }

  implicit val timeout = Timeout(1 second)
  implicit val executionContext = ExecutionContexts.global()

  protected def SyncWithNeighborValue() = {
    neighbors.foreach(n => (n ? MsgGetState()).onComplete(s => s match {
      case scala.util.Success(MsgReturnState(nState: S)) => neighborValues(n) = nState
      case _ => ()
    })) // dispatch ask + sync neighbor values
  }

  protected def DiscoverNeighbors() = {
    (registry ? MsgGetNeighbors(device.name)).onComplete(lst => lst match {
      case scala.util.Success(MsgReturnNeighbors(t)) => {
        Log.info("MY NEIGHBORS ARE " + t.map(_.path.name).mkString(","))
        neighbors = t
      }
      case _ => ()
    }) // dispatch ask + update neighbors
  }

}
