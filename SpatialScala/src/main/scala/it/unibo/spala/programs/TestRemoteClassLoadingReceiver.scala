package it.unibo.spala.programs

import java.net.{URL, URLClassLoader}

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

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
      """.stripMargin), new ByteArrayClassLoader(Thread.currentThread().getContextClassLoader))
  sys.actorOf(Props[RegistryActor], "classLoader")
  sys.actorOf(Props[SimpleActor], "xxx")
}

class SimpleActor extends Actor {
  override def receive: Receive = {
    case x => println("Received: " + x + " of class " + x.getClass)
  }
}

class RegistryActor extends Actor {
  override def receive: Actor.Receive = {
    case (clazz: String, classBytes: Array[Byte]) => {
      println("Received class to load: " + clazz)
      ByteClassLoader.register(clazz, classBytes)
    }
    case x => println("Ops, I got " + x + " but I dont know it")
  }
}


class ByteArrayClassLoader(parent: ClassLoader) extends URLClassLoader(Array[URL](), parent) {
  import ByteClassLoader._
  override
  protected def findClass(name: String) : Class[_] = {
    var result = findLoadedClass(name);
    if (result == null) {
      try {
        result = findSystemClass(name);
      } catch {
        case e => () /* ignore */
      }
    }
    if (result == null) {
      try {
        val classBytes = registeredClasses(name)
        result = defineClass(name, classBytes, 0, classBytes.length);
      } catch {
        case e: Exception => {
          throw new ClassNotFoundException(name);
        }
      }
    }
    result;
  }
}
object ByteClassLoader {
  var registeredClasses: Map[String, Array[Byte]] = Map()

  def register(name: String, classBytes: Array[Byte]) {
    registeredClasses += (name -> classBytes)
  }
}