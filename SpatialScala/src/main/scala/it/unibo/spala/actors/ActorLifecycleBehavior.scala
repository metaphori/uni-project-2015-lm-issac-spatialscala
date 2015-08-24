package it.unibo.spala.actors

import akka.actor.Actor

import scala.concurrent.duration.FiniteDuration

/**
 * Created by: Roberto Casadei 
 * Created on date: 06/08/15.
 */

trait ActorLifecycleBehavior {
  this: Actor =>

  var workInterval: FiniteDuration

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

  def fullWorkingBehavior = workingBehavior.orElse(lifecycleBehavior)

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
  }

  def Stop() = {
    OnStop()
    context.become(initialBehavior)
  }

  def Pause() = {
    OnPause()
    context.become(pausedBehavior)
  }

  def Resume() = {
    OnResume()
    context.become(fullWorkingBehavior)
  }

  def DoJob() = {

  }

  def Shutdown() = {
    OnShutdown()
    context.stop(self)
  }

  def OnStart() = { }
  def OnStop() = { }
  def OnPause() = { }
  def OnResume() = { }
  def OnShutdown() = { }

  def ScheduleNextWorkingCycle(delay: FiniteDuration = workInterval) = {
    context.system.scheduler.scheduleOnce(delay) {
      self ! GoOn
    }
  }

}