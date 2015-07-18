package it.unibo.spala

import java.util.TimerTask

import akka.actor.ActorRef
import it.unibo.spala.math.Point3D
import scala.concurrent.duration._

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */
package object actors {
  /* DATA STRUCTURES */

  case class DevInfo(val id: String, val ref: ActorRef, val position: Option[Point3D]){
  }

  /* FUNCTIONS */

  /**
   * Schedule a task once, ahead in time as expressed by duration
   * Note: actors should use Scheduler.scheduleOnce() instead
   */
  def ScheduleIn(duration: Duration)(body: => Unit): Unit = {
    new java.util.Timer().schedule(new TimerTask {
      override def run(): Unit = {
        body
      }
    }, duration.toMillis)
  }

  /* MESSAGES */

  /**
   * Represents a message to activate an actor.
   */
  object MsgStart extends Serializable

  object MsgStop extends Serializable

  /**
   * Represents a message for asking the state of the recipient actor
   * @tparam S is the type of the state
   */
  case class MsgGetState[S]()

  /**
   * Response message to [[MsgStart]]
   * @param state The state to be returned
   * @tparam S is the type of the state
   */
  case class MsgReturnState[S](state: S)

  /**
   * Self-message used by actors to continue their job
   */
  object GoOn extends Serializable

  case class MsgGetNeighbors(id: String)

  case class MsgReturnNeighbors(nbs: List[ActorRef])

  case class MsgRegister(id: String)

  case class MsgRegisterWithPosition(id: String, position: Point3D)

  case class MsgStartAndRegisterTo(to: ActorRef)

  case class MsgStartWithInputAndRegisterTo[I](input: InputData[I], to: ActorRef)

  case class MsgStartWithRecipient(recipient: ActorRef)

  object MsgGetAll extends Serializable

  object MsgGetAllWithInfo extends Serializable

  case class MsgReturnAllAs[T](all: T)

  case class MsgMove(who: String, newpos: Point3D, isRelative: Boolean)

  case class MsgGetPositionOf[P](who: String)

  case class MsgReturnPositionOf[P](who: String, pos: P)
}

trait CyclicBehavior {
  val workInterval = 500 milliseconds
}

case class InputData[T](val name: String, val value: T)
