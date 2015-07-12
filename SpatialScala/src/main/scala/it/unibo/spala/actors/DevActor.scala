package it.unibo.spala.actors

// Key imports
import it.unibo.spala.math.Point3D
import it.unibo.spala.{CyclicBehavior, Dev}

// Akka imports
import akka.actor.{Actor, ActorRef}

// Utility imports
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.Random

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */

/**
 * The class of device actors.
 * @param device The device behavior being wrapped
 * @param position The location of the device
 * @tparam S // The type of the state of the device
 * @tparam I // The type of the input of the device
 */
class DevActor[S, I](protected var device: Dev[S, (I, Map[ActorRef,S])], position: Point3D) extends Actor with CyclicBehavior {

  /* Key members */

  protected var container: ActorRef = _
  protected var neighbors = Map[ActorRef, Option[S]]()
  protected var input: I = _

  /* Utility members */

  private val Log = Logger(LoggerFactory.getLogger(self.path.name))
  private val rand = new Random()

  /* Implicits and variables */

  import context.dispatcher // Provides the ExecutionContext
  //implicit val timeout = Timeout(1 second) // for the "ask" pattern
  private val nbsDiscoveryInterval = 500 milliseconds

  /* Reactive behaviors */

  def receive = initialBehavior

  protected def initialBehavior: Receive = {
    case MsgStartWithInputAndRegisterTo(i: I, to) => {
      input = i // save initial input
      container = to // save reference to the space manager
      container ! MsgRegisterWithPosition(device.name, position)
      Start()
    }
  }

  protected def workingBehavior: Receive = {
    case _: MsgGetState[S] => sender ! MsgReturnState(device.state)
    case m: MsgMove => {
      container ! MsgMove(device.name, m.newpos, true)
    }
    case GoOn => {
      doJob()
      scheduleNextElaboration()
      MayMove() // simulate mobility
    }
    case MsgReturnNeighbors(t) => UpdateNeighbors(t)
    case MsgReturnState(nState: S) => SaveNeighborState(sender, nState)
  }

  protected def Start(): Unit = {
    DiscoverNeighbors()
    context.become(workingBehavior)
    self ! GoOn
  }

  protected def doJob: Unit => Unit = (Unit) => {
    // Build a map from actors to their state (and filters out those whose state is unknown, i.e., None)
    val map = Map[ActorRef, S](neighbors.filter { case (ref, Some(s)) => true; case _ => false } mapValues (x => x.get) toList: _*)
    // Run the behavior of the device by providing its input and mappings, and store the device in the new state
    device = device.run((input, map))
  }

  protected def scheduleNextElaboration() = {
    context.system.scheduler.scheduleOnce(workInterval) {
      SyncWithNeighborValue()
      self ! GoOn
    }

    // Simulate locality changes by querying the space manager for neighbors
    context.system.scheduler.scheduleOnce(nbsDiscoveryInterval) {
      DiscoverNeighbors()
    }
  }

  // Ask each neighbor for their state
  protected def SyncWithNeighborValue() = {
    neighbors.keys.foreach { n => n ! MsgGetState() }
  }

  // Ask the space manager for the neighbors
  protected def DiscoverNeighbors() = {
    container ! MsgGetNeighbors(device.name)
  }

  // Update current knowledge about neighbors
  protected def UpdateNeighbors(nbs: List[ActorRef]): Unit = {
    lazy val nones: Stream[Option[S]] = None #:: nones
    var newnbs = (nbs zip nones).toMap
    // Keep only the nbs that are included in the return msg
    neighbors = neighbors.filter(kv => nbs.contains(kv._1))
    // Adds the new neighbors that have been discovered
    neighbors ++= newnbs.filter(kv => !neighbors.contains(kv._1))
  }

  // Update current knowledge about the state of a neighbor
  def SaveNeighborState(ref: ActorRef, nState: S): Unit = {
    neighbors += (ref -> Some(nState))
  }

  // To simulate mobility
  protected def MayMove() = {
    if (rand.nextInt(100) < 10) {
      val rx, ry = rand.nextInt(20)
      val isRelative = true
      self ! MsgMove(self.path.name, new Point3D(rx - 10, ry - 10, 0), isRelative)
    }
  }
}
