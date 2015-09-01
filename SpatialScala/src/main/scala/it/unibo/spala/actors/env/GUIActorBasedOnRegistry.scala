package it.unibo.spala.actors.env

import java.awt.{BorderLayout, Color, Graphics}
import javax.swing._

import akka.actor.{Actor, ActorRef}
import it.unibo.spala.CyclicBehavior
import it.unibo.spala.actors._
import it.unibo.spala.math.Point3D

import scala.concurrent.duration._

/**
 * Created by: Roberto Casadei 
 * Created on date: 11/07/15.
 */

class GUIActorBasedOnRegistry(val width: Int, val height: Int, val spaceManager: ActorRef)
  extends Actor with CyclicBehavior {

  /* Local imports and variables */

  import context.dispatcher

  import scala.collection._ // Provides the ExecutionContext
  override val workInterval = 200 milliseconds
  protected val Log = akka.event.Logging(context.system, this)

  /* Key members */

  var refs: mutable.Map[String, ActorRef] = _
  var states: mutable.Map[ActorRef, Option[Any]] = _
  var positions: mutable.Map[ActorRef, Point3D] = _

  /* GUI-related members */
  var frame: JFrame = _
  var canvas: JPanel = _

  /* Behavior */

  def receive: Receive = initialBehavior

  def initialBehavior: Receive = {
    case MsgStart => {
      BuildFrame()
      spaceManager ! MsgGetAllWithInfo
    }
    case MsgReturnAllAs(all: Map[String, DevInfo]) => {
      Log.info(s"Received actors to be monitored: $all")

      refs = mutable.Map(all.values.map(info => (info.id, info.ref)).toList :_*)
      states = mutable.Map(all.values.map(info => (info.ref, None)).toList :_*)
      positions = mutable.Map(all.values.map(info => (info.ref, info.position.get)).toList :_*)

      context.become(drawingBehavior)
      self ! GoOn
    }
  }

  def drawingBehavior: Receive = {
    case GoOn => {
      // 1) Paint according to current info
      DrawAll()
      // 2) Ask status
      AskStatusUpdate()
      // 3) Ask positions
      AskPositionUpdate()
      scheduleNextElaboration()
    }
    case MsgStop => ()
    case MsgReturnState(s: Any) => states(sender) = Some(s)
    case MsgReturnPositionOf(who, pos: Point3D) => positions += (refs(who) -> pos)
  }

  protected def scheduleNextElaboration() = context.system.scheduler.scheduleOnce(workInterval) { self ! GoOn }

  def AskStatusUpdate() = {
    states.foreach(dest => dest._1 ! MsgGetState[Any])
  }

  def AskPositionUpdate() = {
    refs.keys.foreach(id => spaceManager ! MsgGetPositionOf[Point3D](id))
  }

  def DrawAll() = {
    val g = canvas.getGraphics
    // Let's clear then canvas
    g.clearRect(0, 0, this.frame.getWidth, this.frame.getHeight)
    // Then we draw all the devices
    positions.foreach {  case (ref, pos) => { DrawSingle(g, ref, pos) } }
  }

  def DrawSingle(g: Graphics, ref: ActorRef, pos: Point3D) = {
    g.setColor(if(states(ref).getOrElse(None)==0) Color.RED else Color.BLUE)
    g.fillOval(pos.x.toInt-10, pos.y.toInt-10, 5, 5)
    g.setColor(Color.BLACK)
    //g.drawString(ref.path.name + s" (${pos.x.toInt},${pos.y.toInt})", pos.x.toInt, pos.y.toInt-2)
    g.drawString(""+states(ref).getOrElse(None), pos.x.toInt-2, pos.y.toInt+2)
  }

  def BuildFrame(): Unit = {
    frame = new javax.swing.JFrame("GUI: " + self.path.name)
    frame.setSize(width, height)
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

    frame.setVisible(true)

    val panel = new JPanel()
    frame.setContentPane(panel)

    panel.setLayout(new BorderLayout())

    canvas = new JPanel()
    canvas.setBounds(50, 50, width-50, height-50)
    panel.add(canvas, BorderLayout.CENTER)
    canvas.setBackground(Color.WHITE)
  }

}


