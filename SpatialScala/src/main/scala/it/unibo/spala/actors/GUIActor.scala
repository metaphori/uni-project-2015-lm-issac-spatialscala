package it.unibo.spala.actors

import java.awt.{Graphics, BorderLayout, Color}
import javax.swing._

import akka.actor.{Actor, ActorRef}
import it.unibo.spala.CyclicBehavior
import it.unibo.spala.math.Point3D

import scala.concurrent.duration._

/**
 * Created by: Roberto Casadei 
 * Created on date: 11/07/15.
 */

class GUIActor(val width: Int, val height: Int, val spaceManager: ActorRef, actors: Map[String, (ActorRef,Point3D)])
  extends Actor with CyclicBehavior {

  /* Local imports */

  import scala.collection.mutable.Map

  /* Key members */

  val refs: Map[String, ActorRef] = Map(actors.map({ case (n,(r,_)) => (n, r)}).toList :_*)
  val states: Map[ActorRef, Int] = Map(actors.values.map({ case (r,_) => (r, 0)}).toList :_*)
  var positions: Map[ActorRef, Point3D] = Map(actors.values.map({ case (r,p) => (r, p)}).toList :_*)

  /* GUI-related members */
  var frame: JFrame = _
  var canvas: JPanel = _

  /* Behavior */

  def receive: Receive = {
    case MsgStart => {
      BuildFrame()
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
    case MsgReturnState(s:Int) => states(sender) = s
    case MsgReturnPositionOf(who, pos: Point3D) => positions += (refs(who) -> pos)
  }

  def AskStatusUpdate() = {
    states.foreach(dest => dest._1 ! MsgGetState[Int])
  }

  def AskPositionUpdate() = {
    refs.keys.foreach(id => spaceManager ! MsgGetPositionOf[Point3D](id))
  }

  def DrawAll() = {
    val g = canvas.getGraphics
    // Let's clear then canvas
    g.clearRect(0, 0, 600, 600)
    // Then we draw all the devices
    positions.foreach {  case (ref, pos) => { DrawSingle(g, ref, pos) } }
  }

  def DrawSingle(g: Graphics, ref: ActorRef, pos: Point3D) = {
    g.setColor(if(states(ref)==0) Color.RED else Color.BLUE)
    g.fillOval(pos.x.toInt-10, pos.y.toInt-10, 5, 5)
    g.setColor(Color.BLACK)
    //g.drawString(ref.path.name + s" (${pos.x.toInt},${pos.y.toInt})", pos.x.toInt, pos.y.toInt-2)
    g.drawString(""+states(ref), pos.x.toInt-2, pos.y.toInt+2)
  }

  def BuildFrame(): Unit = {
    frame = new javax.swing.JFrame("GUI: " + self.path.name)
    frame.setSize(width, height)
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

    frame.setVisible(true)

    val panel = new JPanel()
    frame.setContentPane(panel)

    panel.getGraphics().setColor(Color.RED)
    panel.getGraphics().drawLine(0, 10, 600, 10)
    panel.setLayout(new BorderLayout())

    canvas = new JPanel()
    canvas.setBounds(0, 100, width, height-100)
    panel.add(canvas, BorderLayout.CENTER)
    canvas.setBackground(Color.WHITE)
  }

  override val workInterval = 200 milliseconds
  protected def scheduleNextElaboration() = ScheduleIn(workInterval) { self ! GoOn }

}
