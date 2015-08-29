package it.unibo.spala.test

import it.unibo.spala.InputNames
import it.unibo.spala.config.{ConfigHelper, GridSettings}
import it.unibo.spala.domain.{DevBehaviorContext, DeviceBehaviors}
import it.unibo.spala.math.Point2D
import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by: Roberto Casadei 
 * Created on date: 28/08/15.
 */

class DeviceBehaviorTests extends FlatSpec with Matchers {

  "GradientBehavior" should "calculate the gradient" in {
    val gradient = DeviceBehaviors.GradientBehavior

    val ctx1 = new DevBehaviorContext(){
      override def getInput[T](name: String): Option[T] = None
      override def getComputationRound: Long = 1
      override def getNeighborsStates[S]: Map[String, Option[S]] =
        Map("a" -> Some(6), "b" -> Some(7), "c" -> Some(7)).asInstanceOf[Map[String,Option[S]]]
    }
    val s1 = gradient.apply(1, ctx1)
    val s2 = gradient.apply(s1, ctx1)
    val s3 = gradient.apply(s2, ctx1)

    s1 shouldEqual 7
    s2 shouldEqual 7
    s3 shouldEqual 7
  }
}