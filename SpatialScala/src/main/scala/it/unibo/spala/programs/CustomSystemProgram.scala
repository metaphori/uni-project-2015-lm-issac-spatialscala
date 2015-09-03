package it.unibo.spala.programs

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.BroadcastGroup
import com.typesafe.config.ConfigFactory
import it.unibo.spala._
import it.unibo.spala.actors._
import it.unibo.spala.actors.device.{DevActorWithRegistry, PositionProviderActor}
import it.unibo.spala.actors.env.{GUIActorBasedOnRegistry, BasicGUIActor}
import it.unibo.spala.actors.system.{MySpaceManager, SysKB}
import it.unibo.spala.config._
import it.unibo.spala.domain.{DBehavior, Dev, DeviceBehaviors}
import it.unibo.spala.math.Point3D

import scala.concurrent.duration._
import scala.util.Random

/**
 * Created by: Roberto Casadei 
 * Created on date: 24/08/15.
 */
object CustomSystemProgram extends App {

  type State = Int

  val settings = Settings[State](
    SystemSettings(
      name = "MyActorSystem",
      executionStrategy = AsyncExecutionStrategy,
      gui = true,
      console = false,
      logLevel = AkkaConfigLogLevel.Info
    ),
    DeviceSettings(
      initialDeviceState = 0,
      inputProvider = (k:Int, name:String, ref:ActorRef, pos:Point3D) => List(InputData(InputNames.Source1, k==19)), // only 50th elem is source
      nameProvider = (k:Int) => "d"+k,
      workInterval = 200 millis,
      mobility = DeviceMobilitySettings(moveProbability = 0.5, moveStep = 40)
    ),
    SpatialSettings(
      proximityThreshold = 100
    ),
    DeviceDistributionSettings(
      n = 100,
      shape = 'grid,
      shapeSettings = GridSettings(nrows = 1, ncols = 30, stepx = 30, stepy = 50, tolerance = 10, offsetx = 100, offsety = 50)
    ),
    DeploymentSettings(
      kind = LocalDeployment
    )
  )

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
  val sys = ActorSystem(systemSettings.name, config)

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
  val devNames = for(i <- 1 to devdistributionSettings.n)
    yield devSettings.nameProvider(i)
  val devsWithPositions = devNames zip positions

  // Creation of device actors
  var k = 0
  val devsInfo =
    devsWithPositions.map { case (name,pos) => {
        val actorProp = Props(classOf[DevActorWithRegistry[State]])
        val actorRef = sys.actorOf(actorProp, "Actor" + name)
        (name, actorRef, pos)
      }
    }

  k = 0
  devsInfo.foreach {
    case (name, a, p) => {
      k += 1
      devSettings.inputProvider(k, name, a, p).foreach(input => a ! MsgWithInput(input))
      a ! MsgWithInput(InputData[State](InputNames.InitialState, 0))
      a ! MsgWithBehavior(new DeviceBehaviors.GradientBehavior)

      val positionInputActor = sys.actorOf(Props(classOf[PositionProviderActor], p), "positionFor"+name)
      positionInputActor ! MsgAddObserver(a)
      positionInputActor ! MsgStart
    }
  }

  // Broadcast router for this group of devices
  var groupActor: ActorRef = _
  groupActor = sys.actorOf(BroadcastGroup(devsInfo.map(_._2.path.toString)).props(), "groupRouter")

  // Starting the system
  if(systemSettings.start) {

    val registry = sys.actorOf(Props(classOf[MySpaceManager], spaceSettings.proximityThreshold), "registry")

    // Starting this group of devices
    groupActor ! MsgStartAndRegisterTo(registry)

    // Starting the GUI actor
    if (systemSettings.gui) {
      val gui = sys.actorOf(Props(classOf[BasicGUIActor], 1000, 600), "gui")
      gui ! MsgStart
      groupActor ! MsgAddObserver(gui)
    }

    /*
    val behavior = new DBehavior[Int]{
      does {
        ctx.getComputationRound.toInt
      }
    }
    */

    val behavior = new DeviceBehaviors.GradientBehavior

    val rand = new Random()
    import sys.dispatcher
    sys.scheduler.scheduleOnce(10 seconds, devsInfo(29)._2, MsgWithInput(InputData(InputNames.Source1, true)))
    sys.scheduler.scheduleOnce(5 seconds, new Runnable(){
      override def run(): Unit = {
        devsInfo.take(2).foreach(d => d._2 ! MsgWithBehavior(behavior))
      }
    })

  }

}
