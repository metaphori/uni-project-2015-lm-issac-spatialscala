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
                    deviceSettings: DeviceSettings[S],
                    spatialSettings: SpatialSettings = SpatialSettings(),
                    devDistributionSettings: DeviceDistributionSettings = DeviceDistributionSettings(),
                    deploymentSettings: DeploymentSettings = DeploymentSettings())

case class SystemSettings(name: String = "MySpatialSystem",
                          prefix: String = "d",
                          start: Boolean = true,
                          executionStrategy: ExecutionStrategy = AsyncExecutionStrategy,
                          gui: Boolean = true,
                          console: Boolean = false,
                          logLevel: String = AkkaConfigLogLevel.Debug)

case class DeviceSettings[S](initialDeviceState: S,
                          inputProvider: (Int, String, ActorRef, Point3D) => InputData[Any],
                          nameProvider: (Int => String) = i => "Device"+i,
                          workInterval: FiniteDuration = 100 millis)

case class SpatialSettings(proximityThreshold: Double = 100)

case class DeviceDistributionSettings (n: Int = 100,
                                       shape: Symbol = 'grid,
                                       shapeSettings: ShapeSettings = GridSettings())

case class DeploymentSettings(val kind: DeploymentKind = LocalDeployment,
                              val distrib: DistributedDeploymentSettings = DistributedDeploymentSettings(),
                              val local: LocalDeploymentSettings = LocalDeploymentSettings())

case class DistributedDeploymentSettings(host: String = "127.0.0.1",
                                         port: Int = 4402,
                                         nodes: Seq[String] = List())

case class LocalDeploymentSettings()

abstract class ShapeSettings

case class GridSettings(nrows: Int = 100,
                        ncols: Int = 100,
                        stepx: Double = 10,
                        stepy: Double = 10,
                        tolerance: Double = 0,
                        offsetx: Double = 0,
                        offsety: Double = 0) extends ShapeSettings

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
    implicit def configToRichConfig(c: Config) = new RichConfig(c)
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