package it.unibo.spala.actors

import akka.actor.{Actor, ActorRef, Props}
import it.unibo.spala.Behaviors._
import it.unibo.spala.math.Point3D
import it.unibo.spala.{DevBehaviorContext, DevContext, InputNames}

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

/**
 * Created by: Roberto Casadei 
 * Created on date: 24/08/15.
 */

class GenericDevActor[S](var computation: Option[DevBehavior[S]] = None)
  extends Actor with DevContext with ActorLifecycleBehavior {

  val name = self.path.toString
  def position: Option[Point3D] = getInput[Point3D](InputNames.Position)
  def initialState: Option[S] = getInput[S](InputNames.InitialState)

  /* Key members */

  protected var computationActor: Option[ActorRef] = None
  protected var mostRecentState: Option[S] = None
  protected var neighbors = Map[ActorRef, Option[S]]()
  val inputs: collection.mutable.Map[String, Any] = collection.mutable.Map()

  /* Utility members */

  private val Log = akka.event.Logging(context.system, this)
  private val rand = new Random()

  /* Implicits and variables */

  import context.dispatcher // Provides the ExecutionContext
  override var workInterval = SysKB.WorkInterval
  private val nbsDiscoveryInterval = SysKB.NbsDiscoveryInterval
  private var round: Long = 0

  /* Reactive behaviors */

  override def setupBehavior: Receive = {
    case MsgWithBehavior(b: DevBehavior[S]) => setDeviceBehavior(b)
    case MsgWithInput(i) => {
      Log.debug(s"Received input '${i.name}' of value '${i.value}'")
      setInput(i.name, i.value)
    }
  }

  def setDeviceBehavior(behavior: DevBehavior[S]) = {
    computation = Some(behavior)

    val initialS = mostRecentState.orElse(this.initialState)

    val devContext = new DevBehaviorContext {
      override def getInput[T](name: String): Option[T] =
        if(inputs.contains(name)) {
          inputs(name) match {
            case v: T => Some(v)
            case _ => None
          }
        }
        else None
      override def getComputationRound: Long = round

      override def getNeighborsStates[S]: Map[String, Option[S]] =
        neighbors.map(elem => elem match { case (k,v: S) => (k.path.toString, v) }).asInstanceOf[Map[String,Option[S]]]

      //      override def getNeighborsStates[T]: Map[String, Option[T]] =
      //      neighbors.map(elem => elem match { case (k,v: S) => (k.path.toString, v) }).asInstanceOf[Map[String,Option[T]]]
    }
    computationActor = Some(this.context.actorOf(
      Props(classOf[DevComputationActor[S]], behavior, devContext, this.workInterval, initialS)
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

  override def DoJob(): Unit = {
    // Build a map from actors to their state (and filters out those whose state is unknown, i.e., None)
    val map = Map[ActorRef, S](neighbors.filter { case (ref, Some(s)) => true; case _ => false } mapValues (x => x.get) toList: _*)
    // Run the behavior of the device by providing its input and mappings, and store the device in the new state

    // Increment round number
    this.round += 1

    propagateStateToNeighbors()
    ScheduleNextWorkingCycle()

    // Simulate locality changes by querying the space manager for neighbors
    context.system.scheduler.scheduleOnce(nbsDiscoveryInterval) {
      DiscoverNeighbors()
    }

    val guiSelection = context.actorSelection(context.system / "gui")
    this.mostRecentState.foreach(s => guiSelection ! MsgReturnDeviceInfo(this.name, s, this.position))
  }

  override def OnStart(): Unit = {
    Log.info(s"Starting (path=${akka.serialization.Serialization.serializedActorPath(self)})")

    this.computationActor.foreach(_ ! MsgStart)
  }

  // Propagate state to all the neighbors
  def propagateStateToNeighbors() = {
    mostRecentState.foreach(s => {
      neighbors.keys.foreach(n => n ! MsgReturnState(s))
    })
  }

  // Ask the space manager for the neighbors
  protected def DiscoverNeighbors() = {
    //container ! MsgGetNeighbors(device.name)
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

  override def setInput[T](name: String, value: T): Unit = inputs += (name -> value)

  override def getNeighborsStates[S]: Map[String, Option[S]] =
    neighbors.map { case (k:ActorRef, v:Option[S]) => (k.path.name, v)  }

  override def getInput[T](name: String): Option[T] =
    inputs.get(name) match { case Some(t:T) => Some(t); case _ => None }

  override def getComputationRound: Long = this.round
}

class GenericDevActorWithRegistry[S]() extends GenericDevActor[S] {

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
  override protected def DiscoverNeighbors() = {
    registry ! MsgGetNeighbors(this.name)
  }

}


class DevComputationActor[S](computation: DevBehavior[S],
                             var ctx: DevBehaviorContext,
                             override var workInterval: FiniteDuration,
                             initialState: Option[S])
  extends Actor with ActorLifecycleBehavior {

  var state: Option[S] = initialState

  override def workingBehavior: Receive = {
    case MsgGetState => sender() ! MsgReturnState(this.state)
  }

  override def DoJob() = {
    this.state = this.state.map(olds => computation.apply(olds, this.ctx))
    this.state.foreach(s => {
      context.parent ! MsgReturnValue(s)
    })
    ScheduleNextWorkingCycle()
  }
}
