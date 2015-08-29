package it.unibo.spala.actors.device

import akka.actor._
import it.unibo.spala.actors._
import it.unibo.spala.actors.patterns.{ActorLifecycleBehavior, ObservableActor}
import it.unibo.spala.actors.system.SysKB
import it.unibo.spala.domain.{DevBehavior, DevBehaviorContext, DevContext}
import it.unibo.spala.math.Point3D
import it.unibo.spala.{InputData, InputNames}

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.util.Random

/**
 * Created by: Roberto Casadei 
 * Created on date: 24/08/15.
 */

class DevActor[S](var computation: Option[DevBehavior[S]] = None)
  extends Actor
  with DevContext
  with ActorLifecycleBehavior
  with ObservableActor {

  val name = self.path.toString
  def position: Option[Point3D] = getInput[Point3D](InputNames.Position)
  def initialState: Option[S] = getInput[S](InputNames.InitialState)

  /* Key members */

  protected var computationActor: Option[ActorRef] = None
  protected var mostRecentState: Option[S] = None
  protected var neighbors = Map[ActorRef, Option[S]]()
  protected var inputs: Map[String, Any] = Map()

  /* Utility members */

  protected val Log = akka.event.Logging(context.system, this)
  private val rand = new Random()

  /* Implicits and variables */

  override var workInterval = SysKB.WorkInterval
  private val nbsDiscoveryInterval = SysKB.NbsDiscoveryInterval
  private var round: Long = 0

  /* Reactive behaviors */

  override def setupBehavior: Receive = basicSetupBehavior.orElse(observersManagementBehavior)

  def basicSetupBehavior: Receive = {
    case MsgWithBehavior(b: DevBehavior[S]) => setDeviceBehavior(b)
    case MsgWithInput(i) => {
      Log.debug(s"Received input '${i.name}' of value '${i.value}'")
      setInput(i.name, i.value)

      // Handling of specific inputs
      i match {
        case InputData(InputNames.Position, pos: Point3D) => UpdatePosition(pos)
        case _ => ()
      }
    }
  }

  def setDeviceBehavior(behavior: DevBehavior[S]) = {
    computation = Some(behavior)

    val initialS = mostRecentState.orElse(this.initialState)

    computationActor.foreach(ca => ca ! PoisonPill)
    computationActor = Some(this.context.actorOf(
      Props(classOf[DevComputationActor[S]], behavior, this.workInterval, initialS), self.path.name+"computation"
    ))
    if(IsWorking())
      computationActor.foreach( _ ! MsgStart )
  }

  override def workingBehavior: Receive = receiveDataBehavior.orElse(setupBehavior)

  def receiveDataBehavior: Receive = {
    case MsgReturnNeighbors(t) => UpdateNeighbors(t)
    // A neighbor is propagating its state
    case MsgReturnState(nState: S) => SaveNeighborState(sender, nState)
    // The computation actor is returning its value
    case MsgReturnValue(state: S) => mostRecentState = Some(state)
  }

  override def OnStart(): Unit = {
    Log.info(s"Starting (path=${akka.serialization.Serialization.serializedActorPath(self)})")

    this.computationActor.foreach(_ ! MsgStart)
  }

  override def DoJob(): Unit = {
    // Build a map from actors to their state (and filters out those whose state is unknown, i.e., None)
    val map = Map[ActorRef, S](neighbors.filter {
      case (ref, Some(s)) => true;
      case _ => false
    } mapValues (x => x.get) toList: _*)

    // Increment round number
    this.round += 1

    // If there is a state, let's notify the interested parties (neighbors and observers)
    mostRecentState.foreach(s => {
      PropagateStateToNeighbors(s)
      NotifyObservers()
    })
    
    ScheduleNextWorkingCycle()

    // Simulate locality changes by querying the space manager for neighbors
    DiscoverNeighbors()
  }

  // Propagate state to all the neighbors
  def PropagateStateToNeighbors(s: S) = {
    neighbors.keys.foreach(n => n ! MsgReturnState(s))
  }

  def UpdatePosition(pos: Point3D): Unit = { }

  protected def DiscoverNeighbors() = {
  }

  // Update current knowledge about neighbors
  protected def UpdateNeighbors(nbs: List[ActorRef]): Unit = {
    lazy val nones: Stream[Option[S]] = None #:: nones
    var newnbs = (nbs zip nones).toMap
    // Keep only the nbs that are included in the return msg
    neighbors = neighbors.filter(kv => nbs.contains(kv._1))
    // Adds the new neighbors that have been discovered
    neighbors ++= newnbs.filter(kv => !neighbors.contains(kv._1))

    computationActor.foreach(_ ! MsgUpdateNeighbors(neighbors.map(elem => elem match { case (k,v: S) => (k.path.toString, v) })))
  }

  // Update current knowledge about the state of a neighbor
  def SaveNeighborState(ref: ActorRef, nState: S): Unit = {
    neighbors += (ref -> Some(nState))
    computationActor.foreach(_ ! MsgUpdateNeighbors(neighbors.map(elem => elem match { case (k,v: S) => (k.path.toString, v) })))
  }

  override def setInput[T](name: String, value: T): Unit = {
    inputs += (name -> value)
    computationActor.foreach(_ ! MsgUpdateInputs(inputs))
  }

  override def getNeighborsStates[S]: Map[String, Option[S]] =
    neighbors.map { case (k:ActorRef, v:Option[S]) => (k.path.name, v)  }

  override def getInput[T](name: String): Option[T] =
    inputs.get(name) match { case Some(t:T) => Some(t); case _ => None }

  override def getComputationRound: Long = this.round

  override def CurrentStateMessage: Any =
    this.mostRecentState.map(s => MsgReturnDeviceInfo(this.name, s, this.position)).get
}

class DevActorWithRegistry[S]() extends DevActor[S] {

  var registry: ActorRef = _

  override def initialBehavior: Receive = {
    val extension: Receive = { case MsgStartAndRegisterTo(reg) => registry = reg; Start() }
    super.initialBehavior.orElse(extension)
  }

  override def OnStart() = {
    super.OnStart()
    registry ! MsgRegisterWithPosition(this.name, this.position.get)
  }

  // Ask the space manager for the neighbors
  override def DiscoverNeighbors() = {
    registry ! MsgGetNeighbors(this.name)
  }

  override def UpdatePosition(pos: Point3D): Unit = {
    if(registry != null) {
      Log.debug(s"Moving to new position $pos")
      registry ! MsgMove(this.name, pos, false)
    }
  }

}


class DevComputationActor[S](computation: DevBehavior[S],
                             override var workInterval: FiniteDuration,
                             initialState: Option[S])
  extends Actor
  with ActorLifecycleBehavior
  with DevBehaviorContext {

  protected val Log = akka.event.Logging(context.system, self.path.toString)
  var neighbors = Map[String, Option[S]]()
  var inputs: Map[String, Any] = Map()

  var state: Option[S] = initialState

  override def workingBehavior: Receive = {
    case MsgUpdateNeighbors(nbs: Map[String,Option[S]]) => neighbors = nbs
    case MsgUpdateInputs(inps: Map[String,Any]) => inputs = inps
    case MsgGetState => sender() ! MsgReturnState(this.state)
  }

  override def DoJob() = {
    this.state = this.state.map(olds => computation(olds, this))
    this.state.foreach(s => {
      context.parent ! MsgReturnValue(s)
    })
    ScheduleNextWorkingCycle()
  }

  override def getNeighborsStates[S]: Map[String, Option[S]] = neighbors.asInstanceOf[Map[String, Option[S]]]

  override def getInput[T](name: String): Option[T] =
    inputs.get(name) match { case Some(t:T) => Some(t); case _ => None }

  override def getComputationRound: Long = 1
}
