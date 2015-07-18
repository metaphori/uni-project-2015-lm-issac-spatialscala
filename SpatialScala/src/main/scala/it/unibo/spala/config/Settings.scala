package it.unibo.spala.config

import akka.actor.ActorRef
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.spala.math.Point3D

/**
 * Created by: Roberto Casadei 
 * Created on date: 16/07/15.
 */

case class Settings[S,I](systemSettings: SystemSettings,
                    deviceSettings: DeviceSettings[S,I],
                    spatialSettings: SpatialSettings,
                    devDistributionSettings: DeviceDistributionSettings,
                    deploymentSettings: DeploymentSettings){ }

case class SystemSettings(name: String = "MySpatialSystem",
                          executionStrategy: ExecutionStrategy,
                          gui: Boolean = false,
                          console: Boolean = true)

case class DeviceSettings[S, I](initialDeviceState: S,
                          inputProvider: (Int, String, ActorRef, Point3D) => I,
                          nameProvider: (Int => String) = i => "Device"+i)

case class SpatialSettings(proximityThreshold: Double = 100)

case class DeviceDistributionSettings (n: Int = 100,
                                       shape: Symbol = 'grid,
                                       shapeSettings: ShapeSettings = GridSettings()) { }

case class DeploymentSettings() { }

abstract class ShapeSettings { }

case class GridSettings(nrows: Int = 100,
                        ncols: Int = 100,
                        stepx: Double = 10,
                        stepy: Double = 10,
                        tolerance: Double = 0,
                        offsetx: Double = 0,
                        offsety: Double = 0) extends ShapeSettings { }

sealed abstract class ExecutionStrategy
case object AsyncExecutionStrategy extends ExecutionStrategy
case object SyncExecutionStrategy extends ExecutionStrategy

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