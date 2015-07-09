package it.unibo.spala

import akka.actor.ActorRef
import scala.concurrent.duration._

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */
package object actors {
  /* MESSAGES */

  /**
   * Represents a message to activate an actor.
   */
  object MsgStart

  object MsgStop

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
  object GoOn

  case class MsgGetNeighbors(id: String)

  case class MsgReturnNeighbors(nbs: List[ActorRef])

  case class MsgRegister(id: String)

  case class MsgStartAndRegisterTo(to: ActorRef)

  case class MsgStartWithInputAndRegisterTo[I](input: I, to: ActorRef)

  case class MsgStartWithRecipient(recipient: ActorRef)

  object MsgGetAll

  case class MsgReturnAllAs[T](all: T)
}

trait CyclicBehavior {
  val interval = 1 second
}
