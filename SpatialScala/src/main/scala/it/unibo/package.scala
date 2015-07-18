package it.unibo

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/**
 * Created by: Roberto Casadei 
 * Created on date: 08/07/15.
 */
package object spala {
  val Log = Logger(LoggerFactory.getLogger("SpalaLogger"))

  type DevMooreBehavior[S] = S => S

  type DevMealyBehavior[S, I] = (S,I) => S

  type MemorylessBehavior[S] = () => S

  implicit def fromMealyToMoore[S, I](mealy: DevMealyBehavior[S,I])(implicit inputProvider: () => I): DevMooreBehavior[S] = {
    mealy(_,inputProvider())
  }

  implicit def fromMemorylessToMoore[S](mless: MemorylessBehavior[S]): DevMooreBehavior[S] = {
    (s: S) => mless()
  }
}
