import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import it.unibo.spala.actors.WorkerActor

/**
 * Created by: Roberto Casadei 
 * Created on date: 13/07/15.
 */
object TestCluster extends App {
    val sys = ActorSystem("MyCluster", ConfigFactory.parseString(
      """
       akka{
          actor {
            provider = "akka.cluster.ClusterActorRefProvider"
          }
          remote {
            enabled-transports = ["akka.remote.netty.tcp"]
            netty.tcp {
              hostname = "127.0.0.1" // the machine you want to run the actor system on
              port = 0  //  the port the actor system should listen on (0 to choose automatically)
            }
          }
          cluster {
              seed-nodes = [
                "akka.tcp://MyCluster@127.0.0.1:2551"]

              auto-down-unreachable-after = 10s
          }
       }
      """.stripMargin))

  (0 to 10).foreach(i =>
  {
    val aref = sys.actorOf(Props[WorkerActor], "worker"+i)
  })
}
