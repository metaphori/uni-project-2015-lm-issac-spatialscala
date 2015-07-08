package it.unibo.spala.actors

import java.util.TimerTask
import scala.concurrent.duration._
import akka.actor.Actor
import com.typesafe.scalalogging.Logger
import it.unibo.spala.Dev
import org.slf4j.LoggerFactory

trait CyclicBehavior {
  val interval = 1 second
}

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */
class DevActor[S](var device: Dev[S]) extends Actor with CyclicBehavior {

  val Log = Logger(LoggerFactory.getLogger(self.path.name))

  def receive = initialBehavior

  def initialBehavior: Receive = {
    case MsgStart => {
      //context.setReceiveTimeout(Duration(1000, TimeUnit.MILLISECONDS))
      Log.debug("Becoming a worker...")
      context.become(workingBehavior)
      self ! GoOn
    }
  }

  def workingBehavior: Receive = {
    case _:MsgGetState[S] => { Log.debug(sender.path.name + " asked my state") ; sender ! MsgReturnState(device.state) }
    case GoOn => workingTask()
  }

  def workingTask: Unit => Unit = (Unit) => {
    val oldState = device.state
    device = device.run
    val newState = device.state
    Log.debug(s"Step: $oldState => $newState")
    scheduleNextElaboration()
  }

  def scheduleNextElaboration() = { new java.util.Timer().schedule(new TimerTask {
    override def run(): Unit = self ! GoOn
  }, interval.toMillis) }

}
