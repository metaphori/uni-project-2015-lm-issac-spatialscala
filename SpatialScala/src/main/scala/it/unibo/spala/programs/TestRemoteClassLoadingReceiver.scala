package it.unibo.spala.programs

import java.net.{URL, URLClassLoader}

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import it.unibo.spala.actors.support.{ClassLoaderActor, CustomClassLoader}

/**
 * Created by: Roberto Casadei 
 * Created on date: 13/07/15.
 */
object TestRemoteClassLoadingReceiver extends App {
    val sys = ActorSystem("MySystem", ConfigFactory.parseString(
      """
       akka{
          actor {
            provider = "akka.remote.RemoteActorRefProvider"
          }
          remote {
            enabled-transports = ["akka.remote.netty.tcp"]
            netty.tcp {
              hostname = "192.168.1.10" // the machine you want to run the actor system on
              port = 6000  //  the port the actor system should listen on (0 to choose automatically)
            }
          }
       }
      """.stripMargin), new CustomClassLoader(Thread.currentThread().getContextClassLoader))
  sys.actorOf(Props[ClassLoaderActor], "classLoader")
  sys.actorOf(Props[SimpleActor], "xxx")
}

class SimpleActor extends Actor {
  override def receive: Receive = {
    case x => println("Received: " + x + " of class " + x.getClass)
  }
}