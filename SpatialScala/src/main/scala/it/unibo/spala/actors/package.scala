package it.unibo.spala

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
}
