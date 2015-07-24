package it.unibo.spala.actors

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.BroadcastGroup
import com.typesafe.config.ConfigFactory
import it.unibo.spala.Behaviors.DevBehavior
import it.unibo.spala._
import it.unibo.spala.config._

import scala.concurrent.duration._

/**
 * Created by: Roberto Casadei 
 * Created on date: 15/07/15.
 */

class MySpaceManager(threshold: Double) extends SimpleSpaceManagerActor {
  override val proximityThreshold: Double = threshold
}

class SpatialSystem[S](val settings: Settings[S],
                       val behavior: DevBehavior[S]) {

    val systemSettings = settings.systemSettings
    val devSettings = settings.deviceSettings
    val spaceSettings = settings.spatialSettings
    val devdistributionSettings = settings.devDistributionSettings
    val shapeSettings = devdistributionSettings.shapeSettings

    val distrib = settings.deploymentSettings.kind == DistributedDeployment
    val ddSettings = settings.deploymentSettings.distrib
    val ldSettings = settings.deploymentSettings.local

    var config = if(!distrib) ConfigFactory.defaultApplication() else ConfigFactory.load("remote_application")
    var deploymentSpecificConfig = ""
    if(distrib){
      deploymentSpecificConfig =
        """
          akka.remote.netty.tcp.hostname = %s
          akka.remote.netty.tcp.port = %d
        """.stripMargin.format(ddSettings.host, ddSettings.port)
    }

    config = ConfigFactory.parseString(
      """
        akka.loglevel= %s
      """.stripMargin.format(
            systemSettings.logLevel) + "\n" + deploymentSpecificConfig).withFallback(config)

    // Setting system-wide parameters
    SysKB.WorkInterval = devSettings.workInterval

    val sys = ActorSystem(systemSettings.name, config)

    val positions = devdistributionSettings.shape match {
      case 'grid if shapeSettings.isInstanceOf[GridSettings] =>
        ConfigHelper.GridLocations(shapeSettings.asInstanceOf[GridSettings])
    }

    // CREATE COMPONENTS
    val devs = for(i <- 1 to devdistributionSettings.n)
      yield new Dev[S](devSettings.nameProvider(i), devSettings.initialDeviceState, behavior)
    val devsWithPositions = devs zip positions

    var k = 0
    val devsInfo =
      devsWithPositions.map { case (dev,pos) => {
          val actorProp = Props(classOf[DevActor[S]], dev, pos)
          val actorRef = sys.actorOf(actorProp, "Actor" + dev.name)
          (dev.name, actorRef, pos)
        }
      }

    // START THE SYSTEM
    k = 0
    devsInfo.foreach { case (name, a, p) => {
        k += 1;
        val input = devSettings.inputProvider(k, name, a, p)
        a ! MsgWithInput(input)
      }
    }

    var groupActor: ActorRef = _
    if(devdistributionSettings.n > 0)
      groupActor = sys.actorOf(BroadcastGroup(devsInfo.map(_._2.path.toString)).props(), "groupRouter")

    if(systemSettings.start) {

      val registry = sys.actorOf(Props(classOf[MySpaceManager], spaceSettings.proximityThreshold), "registry")

      if(devdistributionSettings.n>0) {
        groupActor ! MsgStartAndRegisterTo(registry)
      }

      if(distrib){
        ddSettings.nodes.foreach(node => {
          sys.actorSelection(s"akka.tcp://${systemSettings.name}@${node}/user/groupRouter") ! MsgStartAndRegisterTo(registry)
        })
      }

      if (systemSettings.console) {
        val sampler = sys.actorOf(Props[SamplerActor])
        sampler ! MsgStartWithRecipient(registry)
      }

      if (systemSettings.gui) {
        val gui = sys.actorOf(Props(classOf[GUIActor], 800, 600, registry))
        import sys.dispatcher
        sys.scheduler.scheduleOnce(1 second) {
          gui ! MsgStart
        } // give time to devices to register
      }

    }
}
