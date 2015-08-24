package it.unibo.spala.actors

// Key imports

import it.unibo.spala.Behaviors.DevBehavior
import it.unibo.spala.math.Point3D
import it.unibo.spala.{CyclicBehavior, Dev, DevBehaviorContext, DevContext}

import scala.concurrent.duration.FiniteDuration

// Akka imports
import akka.actor.{Actor, ActorRef, Props}

// Utility imports
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
 */
class DevActor[S](protected var device: Dev[S],
                  position: Point3D)
  extends Actor with DevContext with CyclicBehavior {

  /* Key members */

  protected var container: ActorRef = _
  protected var neighbors = Map[ActorRef, Option[S]]()

  /* Utility members */

  private val Log = akka.event.Logging(context.system, this)
  private val rand = new Random()

  /* Implicits and variables */

  import context.dispatcher // Provides the ExecutionContext
  //implicit val timeout = Timeout(1 second) // for the "ask" pattern
  override val workInterval = SysKB.WorkInterval
  private val nbsDiscoveryInterval = SysKB.NbsDiscoveryInterval
  private var round: Long = 0

  /* Reactive behaviors */

  def receive = initialBehavior

  protected def initialBehavior: Receive = {
    case MsgStartWithInputAndRegisterTo(i, to) => {
      setInput(i.name, i.value) // save initial input
      container = to // save reference to the space manager
      container ! MsgRegisterWithPosition(device.name, position)
      Start()
    }
    case MsgWithInput(i) => setInput(i.name, i.value)
    case MsgStartAndRegisterTo(to) => {
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
      propagateStateToNeighbors()
      scheduleNextElaboration()
    }
    case MsgReturnNeighbors(t) => UpdateNeighbors(t)
    case MsgReturnState(nState: S) => SaveNeighborState(sender, nState)
  }

  protected def Start(): Unit = {
    val physicalPath = akka.serialization.Serialization.serializedActorPath(self)
    Log.info(s"Starting (path=$physicalPath)")
    DiscoverNeighbors()
    if(SysKB.MobileDevices) context.system.scheduler.schedule(SysKB.MoveInterval, SysKB.MoveInterval)(MayMove())
    context.become(workingBehavior)
    self ! GoOn
  }

  protected def doJob: Unit => Unit = (Unit) => {
    // Build a map from actors to their state (and filters out those whose state is unknown, i.e., None)
    val map = Map[ActorRef, S](neighbors.filter { case (ref, Some(s)) => true; case _ => false } mapValues (x => x.get) toList: _*)
    // Run the behavior of the device by providing its input and mappings, and store the device in the new state
    device = device.run(this)
    // Increment round number
    this.round += 1
  }

  protected def scheduleNextElaboration() = {
    context.system.scheduler.scheduleOnce(workInterval) {
      //SyncWithNeighborValue()
      self ! GoOn
    }

    // Simulate locality changes by querying the space manager for neighbors
    context.system.scheduler.scheduleOnce(nbsDiscoveryInterval) {
      DiscoverNeighbors()
    }
  }

  // Ask each neighbor for their state
  /*
  protected def SyncWithNeighborValue() = {
    neighbors.keys.foreach { n => n ! MsgGetState() }
  }
  */

  // Propagate state to all the neighbors
  def propagateStateToNeighbors() = {
    neighbors.keys.foreach( n => n ! MsgReturnState(device.state) )
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
    if (rand.nextDouble() < SysKB.MoveProbability) {
      val rx, ry = rand.nextDouble() * SysKB.MaxMoveStep
      val isRelative = true
      self ! MsgMove(self.path.name, new Point3D(rx - SysKB.MaxMoveStep/2, ry - SysKB.MaxMoveStep/2, 0), isRelative)
    }
  }

  val inputs: collection.mutable.Map[String, Any] = collection.mutable.Map()
  override def setInput[T](name: String, value: T): Unit = inputs += (name -> value)

  override def getNeighborsStates[S]: Map[String, Option[S]] =
    neighbors.map { case (k:ActorRef, v:Option[S]) => (k.path.name, v)  }

  override def getInput[T](name: String): Option[T] =
    inputs.get(name) match { case Some(t:T) => Some(t); case _ => None }

  override def getComputationRound: Long = this.round
}


class GenericDevActor[S](var computation: Option[DevBehavior[S]])
  extends Actor with DevContext with ActorLifecycleBehavior {

  val name = self.path.toString

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
  override val workInterval = SysKB.WorkInterval
  private val nbsDiscoveryInterval = SysKB.NbsDiscoveryInterval
  private var round: Long = 0

  /* Reactive behaviors */

  override def setupBehavior: Receive = {
    case MsgWithBehavior(b: DevBehavior[S]) => setDeviceBehavior(b)
    case MsgWithInput(i) => setInput(i.name, i.value)
  }

  def setDeviceBehavior(behavior: DevBehavior[S]) = {
    computation = Some(behavior)
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
        neighbors.keys.map(k => (k.path.toString, neighbors(k))).toMap[String, Option[S]]
    }
    computationActor = Some(this.context.actorOf(Props(classOf[DevComputationActor], devContext, this.workInterval, mostRecentState), "computation"))
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
  }

  override def OnStart(): Unit = {
    val physicalPath = akka.serialization.Serialization.serializedActorPath(self)
    Log.info(s"Starting (path=$physicalPath)")
    if(SysKB.MobileDevices) context.system.scheduler.schedule(SysKB.MoveInterval, SysKB.MoveInterval)(MayMove())
  }

  // Propagate state to all the neighbors
  def propagateStateToNeighbors() = {
    //neighbors.keys.foreach( n => n ! MsgReturnState(device.state) )
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

  // To simulate mobility
  protected def MayMove() = {
    if (rand.nextDouble() < SysKB.MoveProbability) {
      val rx, ry = rand.nextDouble() * SysKB.MaxMoveStep
      val isRelative = true
      self ! MsgMove(self.path.name, new Point3D(rx - SysKB.MaxMoveStep/2, ry - SysKB.MaxMoveStep/2, 0), isRelative)
    }
  }

  override def setInput[T](name: String, value: T): Unit = inputs += (name -> value)

  override def getNeighborsStates[S]: Map[String, Option[S]] =
    neighbors.map { case (k:ActorRef, v:Option[S]) => (k.path.name, v)  }

  override def getInput[T](name: String): Option[T] =
    inputs.get(name) match { case Some(t:T) => Some(t); case _ => None }

  override def getComputationRound: Long = this.round
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
    this.state = Some(computation.apply(this.state.get, this.ctx))
    context.parent ! MsgReturnValue(this.state.get)
    ScheduleNextWorkingCycle()
  }
}