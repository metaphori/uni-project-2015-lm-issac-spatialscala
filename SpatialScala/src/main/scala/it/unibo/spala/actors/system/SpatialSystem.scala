package it.unibo.spala.actors.system

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.BroadcastGroup
import com.typesafe.config.ConfigFactory
import it.unibo.spala._
import it.unibo.spala.actors._
import it.unibo.spala.actors.device.{DevActorWithRegistry, PositionProviderActor}
import it.unibo.spala.actors.env.{BasicGUIActor, SamplerActor}
import it.unibo.spala.actors.support.{ClassLoaderActor, ByteArrayClassLoader}
import it.unibo.spala.config._
import it.unibo.spala.domain.{DevBehavior, DBehavior, Dev}

/**
 * Created by: Roberto Casadei 
 * Created on date: 15/07/15.
 */

class MySpaceManager(threshold: Double) extends SimpleSpaceManagerActor {
  override val proximityThreshold: Double = threshold
}

class SpatialSystem[S](val settings: Settings[S],
                       val behaviorGen: () => DevBehavior[S]) {

    val systemSettings = settings.systemSettings
    val devSettings = settings.deviceSettings
    val spaceSettings = settings.spatialSettings
    val devdistributionSettings = settings.devDistributionSettings
    val shapeSettings = devdistributionSettings.shapeSettings

    val distrib = settings.deploymentSettings.kind == DistributedDeployment
    val ddSettings = settings.deploymentSettings.distrib
    val ldSettings = settings.deploymentSettings.local

    // System configuration: deployment, logging, ...
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
      """.stripMargin.format(systemSettings.logLevel) + "\n" + deploymentSpecificConfig).withFallback(config)

    // Setting system-wide parameters
    SysKB.WorkInterval = devSettings.workInterval
    SysKB.MobileDevices = devSettings.mobility.canMove
    SysKB.MaxMoveStep = devSettings.mobility.moveStep
    SysKB.MoveProbability = devSettings.mobility.moveProbability

    // Creation of the actor system
    val sys = ActorSystem(systemSettings.name, config, new ByteArrayClassLoader(Thread.currentThread().getContextClassLoader))

    // Positions of the devices
    val positions = devdistributionSettings.shape match {
      case 'grid if shapeSettings.isInstanceOf[GridSettings] =>
        ConfigHelper.GridLocations(
          shapeSettings.asInstanceOf[GridSettings],
          shapeSettings.seed.orElse(systemSettings.seed).getOrElse(System.currentTimeMillis()))
      case 'random if shapeSettings.isInstanceOf[SimpleRandomSettings] =>
        ConfigHelper.RandomLocations(
          shapeSettings.asInstanceOf[SimpleRandomSettings],
          devdistributionSettings.n,
          shapeSettings.seed.orElse(systemSettings.seed).getOrElse(System.currentTimeMillis()))
    }

    // Creation of devices
    val devnames = for(i <- 1 to devdistributionSettings.n)
      yield devSettings.nameProvider(i)
    val devsWithPositions = devnames zip positions

    // Creation of device actors
    var k = 0
    val devsInfo =
      devsWithPositions.map { case (devname,pos) => {
          val actorProp = Props(classOf[DevActorWithRegistry[S]])
          val actorRef = sys.actorOf(actorProp, systemSettings.prefix + devname)
          (devname, actorRef, pos)
        }
      }

    // Provide input to the devices
    k = 0
    devsInfo.foreach { case (name, a, p) => {
      k += 1
      devSettings.inputProvider(k, name, a, p).foreach(input => a ! MsgWithInput(input))
      a ! MsgWithInput(InputData[S](InputNames.InitialState, devSettings.initialDeviceState))
      a ! MsgWithBehavior(behaviorGen())

      val positionInputActor = sys.actorOf(Props(classOf[PositionProviderActor], p), "positionFor"+name)
      positionInputActor ! MsgAddObserver(a)
      positionInputActor ! MsgStart
      }
    }

    // Broadcast router for this group of devices
    var groupActor: Option[ActorRef] = None
    if(devdistributionSettings.n > 0) {
      groupActor = Some(sys.actorOf(BroadcastGroup(devsInfo.map(_._2.path.toString)).props(), "groupRouter"))
    }

    // Create a class loader actor to support code mobility
    sys.actorOf(Props[ClassLoaderActor], "classloader")

    // Starting the system
    if(systemSettings.start) {
      StartSystem()
    }

  def StartSystem() = {
    // Creation of Space Manager, which keeps track of devices and their spatial positions
    val registry = sys.actorOf(Props(classOf[MySpaceManager], spaceSettings.proximityThreshold), "registry")

    // Starting this group of devices
    groupActor.foreach( _ ! MsgStartAndRegisterTo(registry) )

    // Starting remote groups of devices on the provided nodes
    if(distrib){
      ddSettings.nodes.foreach(node => {
        sys.actorSelection(s"akka.tcp://${systemSettings.name}@${node}/user/groupRouter") ! MsgStartAndRegisterTo(registry)
      })
    }

    // Starting a console actor which periodically prints out the states of the devices
    if (systemSettings.console) {
      val sampler = sys.actorOf(Props[SamplerActor])
      sampler ! MsgStartWithRecipient(registry)
    }

    // Starting the GUI actor
    if (systemSettings.gui) {
      val gui = sys.actorOf(Props(classOf[BasicGUIActor], 800, 600), "gui")
      gui ! MsgStart
      ddSettings.nodes.foreach(node => {
        sys.actorSelection(s"akka.tcp://${systemSettings.name}@${node}/user/groupRouter") ! MsgAddObserver(gui)
      })
      groupActor.foreach( _ ! MsgAddObserver(gui) )
    }
  }

}
