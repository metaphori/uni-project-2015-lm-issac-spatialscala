package it.unibo.spala.actors.patterns

import akka.actor.Actor
import it.unibo.spala.actors._

import scala.concurrent.duration.FiniteDuration

/**
 * Created by: Roberto Casadei 
 * Created on date: 06/08/15.
 */

abstract trait LifecycleState
case object LSInitial extends LifecycleState
case object LSStarted extends LifecycleState
case object LSPaused extends LifecycleState
case object LSResumed extends LifecycleState
case object LSStopped extends LifecycleState
case object LSFinalized extends LifecycleState

trait ActorLifecycleBehavior {
  actor: Actor =>

  var workInterval: FiniteDuration
  var lifecycleState: LifecycleState = LSInitial

  import context.dispatcher

  override def receive: Receive = initialBehavior

  def initialBehavior: Receive =
    activationBehavior.orElse(setupBehavior).orElse({
      case MsgShutdown => Shutdown()
    })

  def activationBehavior: Receive = {
    case MsgStart => Start()
  }

  def setupBehavior: Receive = Map.empty

  def workingBehavior: Receive = Map.empty

  def fullWorkingBehavior = workingBehavior.orElse(lifecycleBehavior).orElse(fallbackBehavior)

  def Unhandled(x: Any): Unit = { Console.println("Unhandled " + x); throw new Exception("Unhandled msg " + x) }

  def fallbackBehavior: Receive = {
    case x => Unhandled(x)
  }

  def lifecycleBehavior: Receive = {
    case GoOn => {
      DoJob()
    }
    case MsgPause => Pause()
    case MsgStop => Stop()
    case MsgShutdown => Shutdown()
  }

  def pausedBehavior: Receive = {
    case MsgResume => Resume()
    case MsgStop => Stop()
    case MsgShutdown => Shutdown()
  }

  def Start() = {
    OnStart()
    context.become(fullWorkingBehavior)
    self ! GoOn
    this.lifecycleState = LSStarted
  }

  def Stop() = {
    OnStop()
    context.become(initialBehavior)
    this.lifecycleState = LSStopped
  }

  def Pause() = {
    OnPause()
    context.become(pausedBehavior)
    this.lifecycleState = LSPaused
  }

  def Resume() = {
    OnResume()
    context.become(fullWorkingBehavior)
    this.lifecycleState = LSResumed
  }

  def DoJob() = {

  }

  def Shutdown() = {
    OnShutdown()
    context.stop(self)
    this.lifecycleState = LSFinalized
  }

  def OnStart() = { }
  def OnStop() = { }
  def OnPause() = { }
  def OnResume() = { }
  def OnShutdown() = { }

  def ScheduleNextWorkingCycle(delay: FiniteDuration = workInterval) = {
    // "self" is safe to close over
    context.system.scheduler.scheduleOnce(delay) {
      self ! GoOn
    }
  }

  def IsWorking() = {
    this.lifecycleState == LSStarted || this.lifecycleState == LSResumed
  }

}
