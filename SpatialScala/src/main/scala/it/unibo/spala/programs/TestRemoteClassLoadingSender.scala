package it.unibo.spala.programs

import java.nio.file._
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory
import it.unibo.spala.actors.GoOn
import akka.pattern.ask
import it.unibo.spala.actors.support.LoadClassBytes

import scala.concurrent.duration.Duration

/**
 * Created by: Roberto Casadei 
 * Created on date: 20/07/15.
 */
object TestRemote extends App {

  val sys = ActorSystem("MySystem", ConfigFactory.parseString(
    """
       akka{
          actor {
            provider = "akka.remote.RemoteActorRefProvider"
          }
          remote {
            enabled-transports = ["akka.remote.netty.tcp"]
            netty.tcp {
              hostname = "192.168.1.9" // the machine you want to run the actor system on
              port = 2555  //  the port the actor system should listen on (0 to choose automatically)
            }
          }
       }
    """.stripMargin))

  /*
  println(akka.serialization.Serialization.serializedActorPath(aref))
  aref ! GoOn
  val aref = sys.actorOf(Props[VerySimpleActor], "verySimpleActor")
  */

  val remote = ActorSystem()

  val mymsg: (String, Array[Byte]) = LoadClassBytes(classOf[MyMsg]).head
  implicit val timeout: akka.util.Timeout = Duration.create(5, TimeUnit.SECONDS)
  import sys.dispatcher
  sys.actorSelection("akka.tcp://MySystem@192.168.1.10:6000/user/classLoader").
    resolveOne().
    onSuccess {
     case (ref: ActorRef) => {
       println("Resolved registry to actorRef " + ref)
       (ref ? mymsg).onComplete(
         _ => sys.actorSelection("akka.tcp://MySystem@192.168.1.10:6000/user/xxx") ! MyMsg(10)
       )
     }
    }

}

case class MyMsg(x: Int)


class VerySimpleActor extends Actor {
  def receive: Receive = {
    case GoOn => println("Hi there")
  }
}
