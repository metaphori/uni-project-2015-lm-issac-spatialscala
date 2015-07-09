import akka.actor.{Props, ActorRef, ActorSystem}
import it.unibo.spala.actors._
import it.unibo.spala.{Dev, Log}

object MainProgram extends App {
  org.apache.log4j.BasicConfigurator.configure()
  Log.info("Main program started")

  val sys = ActorSystem("MyActorSystem")

  Log.info("Actor system created")

  type Device = Dev[Int, (Boolean, Map[ActorRef, Int])]
  type DeviceActor = DevActor[Int, (Boolean, Map[ActorRef, Int])]
  val behavior = (state:Int, input:(Boolean, Map[ActorRef, Int])) => {
    if(input._1) { 0 }
    else if(!input._2.isEmpty) input._2.values.min + 1
    else -1
  }

  val devs = for(i <- 1 to 10) yield new Device("D"+i, 0, behavior)
  val registry = sys.actorOf(Props[SimpleRegistryActor])
  val actors = devs.map(d => sys.actorOf(Props(classOf[DeviceActor], d), "Actor"+d.name))
  actors.head ! MsgStartWithInputAndRegisterTo(true, registry)
  actors.tail.foreach(a => a ! MsgStartWithInputAndRegisterTo(false, registry) )

  val sampler = sys.actorOf(Props[SamplerActor])
  sampler ! MsgStartWithRecipient(registry)

  /*
  trait SlowerCyclicBehavior extends CyclicBehavior {
    override val interval = 5000 milliseconds
  }
  class SlowerDevice(dev: Device) extends DevActor[Int, Boolean](dev) with SlowerCyclicBehavior

  val fdev1: Device = Dev(0, behavior)
  val fdev2: Device = Dev(0, behavior)

  val registry = sys.actorOf(Props[RegistryActor])
  val dev1 = sys.actorOf(Props(classOf[Device], fdev1), "dev1")
  val dev2 = sys.actorOf(Props(classOf[SlowerDevice], fdev2), "dev2")

  Log.info("Actors created")
  Log.info("Starting actors...")

  dev1 ! MsgStartAndRegisterTo(registry)
  dev2 ! MsgStartAndRegisterTo(registry)


  val devs = List(dev1,dev2)
  implicit val timeout = Timeout(5 second)
  implicit val executionContext = ExecutionContexts.global()
  var k = 0
  while(k<10) {
    devs.foreach(d => (d ? MsgGetState[Int]).onComplete(i => Log.info(s"Sampling state = ${i}")))
    k+=1
    Thread.sleep(707)
  }
  */
}