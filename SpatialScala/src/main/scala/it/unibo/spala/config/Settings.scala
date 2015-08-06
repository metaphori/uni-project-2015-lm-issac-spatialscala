package it.unibo.spala.config

import akka.actor.ActorRef
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.spala.InputData
import it.unibo.spala.math.Point3D

import scala.concurrent.duration._

/**
 * Created by: Roberto Casadei 
 * Created on date: 16/07/15.
 */

case class Settings[S](systemSettings: SystemSettings = SystemSettings(),
                    deviceSettings: DeviceSettings[S] = DeviceSettings[S](),
                    spatialSettings: SpatialSettings = SpatialSettings(),
                    devDistributionSettings: DeviceDistributionSettings = DeviceDistributionSettings(),
                    deploymentSettings: DeploymentSettings = DeploymentSettings())

case class SystemSettings(name: String = "MySpatialSystem",
                          prefix: String = "d",
                          seed: Option[Long] = None,
                          start: Boolean = true,
                          executionStrategy: ExecutionStrategy = AsyncExecutionStrategy,
                          gui: Boolean = true,
                          console: Boolean = false,
                          logLevel: String = AkkaConfigLogLevel.Debug)

case class DeviceSettings[S](initialDeviceState: S = null.asInstanceOf[S],
                          inputProvider: (Int, String, ActorRef, Point3D) => InputData[Any] = (i,s,a,p) => null,
                          nameProvider: (Int => String) = i => "Device"+i,
                          workInterval: FiniteDuration = 100 millis,
                          mobility: DeviceMobilitySettings = DeviceMobilitySettings())

case class DeviceMobilitySettings(canMove: Boolean = true,
                                  moveStep: Double = 20,
                                  moveProbability: Double = 0.1)

case class SpatialSettings(proximityThreshold: Double = 100)

case class DeviceDistributionSettings (n: Int = 100,
                                       shape: Symbol = 'random,
                                       shapeSettings: ShapeSettings = SimpleRandomSettings())

case class DeploymentSettings(kind: DeploymentKind = LocalDeployment,
                              distrib: DistributedDeploymentSettings = DistributedDeploymentSettings(),
                              local: LocalDeploymentSettings = LocalDeploymentSettings())

case class DistributedDeploymentSettings(host: String = "127.0.0.1",
                                         port: Int = 0,
                                         nodes: Seq[String] = List())

case class LocalDeploymentSettings()

abstract class ShapeSettings(val seed: Option[Long] = None)

case class GridSettings(nrows: Int = 10,
                        ncols: Int = 10,
                        stepx: Double = 100,
                        stepy: Double = 80,
                        tolerance: Double = 0,
                        offsetx: Double = 0,
                        offsety: Double = 0) extends ShapeSettings

case class SimpleRandomSettings(min: Double = 0,
                                max: Double = 1000) extends ShapeSettings

sealed abstract class DeploymentKind
case object LocalDeployment extends DeploymentKind
case object DistributedDeployment extends DeploymentKind

sealed abstract class ExecutionStrategy
case object AsyncExecutionStrategy extends ExecutionStrategy
case object SyncExecutionStrategy extends ExecutionStrategy

object AkkaConfigLogLevel {
  val Debug = "DEBUG"
  val Warn = "WARNING"
  val Info = "INFO"
  val None = "OFF"
}

// DRAFT
/*
Doesn't work because ...
val d = classOf[Double]
val s = classOf[String]
d match { case s => 1 } // returns, unexpectedly, 1 instead of MatchError
 */
class RichConfig(c: Config){
  def getOption[T](classType: Class[T], path: String): Option[T] = {
    if(!c.hasPath(path)) None
    else {
      val classDouble = classOf[Double]
      val classString = classOf[String]
      classType match {
        case classString => Some(c.getString(path).asInstanceOf[T])
        case classDouble => Some(c.getDouble(path).asInstanceOf[T])
        case _ => None
      }
    }
  }
}

// DRAFT
object SettingsBuilder {

  def buildFromConfig(config: Config) = {
    import scala.collection.JavaConversions._
    implicit def configToRichConfig(c: Config): RichConfig = new RichConfig(c)
    //val actorSys = ActorSystem (name)

    var cfg = config.withFallback(ConfigFactory.load ("spatial_reference")).resolve()

    val default_tolerance = cfg.getDouble("default_tolerance")

    val devdistribution = cfg.getConfig("place-devices")

    cfg.getObject("place-devices").foreach({ case (group, gobj) =>
      val c = devdistribution.getConfig(group)
      val n = c.getOption(classOf[Int], "n")
      val shape = c.getOption(classOf[String], "shape")
      val distance = c.getOption(classOf[Double], "distance")
      val tolerance = c.getOption(classOf[Double], "tolerance").getOrElse(default_tolerance)
      println(s"N $n Shape $shape Distance $distance Tolerance $tolerance")
    })
  }

}