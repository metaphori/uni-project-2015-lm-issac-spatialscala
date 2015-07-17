package it.unibo.spala.actors

import akka.actor._
import com.typesafe.config.ConfigFactory

class SimpleActor extends Actor {
  def receive: Receive = {
    case GoOn => println("Hi there")
  }
}

class AnotherActor extends Actor {
  def receive: Receive = {
    case GoOn => {
      println("I live in " +akka.serialization.Serialization.serializedActorPath(self))
    }
  }
}

object TestRemote extends App {

  val sys = ActorSystem("MyRemoteAS", ConfigFactory.parseString(
    """
       akka{
          actor {
            provider = "akka.remote.RemoteActorRefProvider"
          }
          remote {
            enabled-transports = ["akka.remote.netty.tcp"]
            netty.tcp {
              hostname = "127.0.0.1" // the machine you want to run the actor system on
              port = 2555  //  the port the actor system should listen on (0 to choose automatically)
            }
          }
       }
    """.stripMargin))

  val aref = sys.actorOf(Props[SimpleActor], "simpleActor")
  println(akka.serialization.Serialization.serializedActorPath(aref))
  aref ! GoOn

}