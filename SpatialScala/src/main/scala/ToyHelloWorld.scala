import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props

class ActorPing extends Actor {
  def receive = {
    case "start" => {
      println("FIRST PING")
      context.actorSelection("/user/pong") ! "ping"
    }
    case "pong" => {
      println("ping")
      sender ! "ping"
      Thread.sleep(1500)
    }
  }
}

class ActorPong extends Actor {
  def receive = {
    case "ping" => {
      println("pong")
      sender ! "pong"
      Thread.sleep(100)
    }
  }
}

object MyMain extends App {
    println("Hello world: " + args.foreach { x => println(x) })
    
    val sys = ActorSystem("MyActorSystem")
    val producer = sys.actorOf(Props[ActorPing], "ping")
    val consumer = sys.actorOf(Props[ActorPong], "pong")
    
    producer ! "start"
}