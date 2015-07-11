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

class GUIActor(val width: Int, val height: Int, actors: Map[ActorRef,Point3D]) extends Actor with CyclicBehavior {

  /* Local imports */

  import scala.collection.mutable.Map

  /* Key members */

  val devs: Map[ActorRef, Int] = Map(actors.keys.map(r => (r, 0)).toList :_*)
  var poss = actors

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
    case MsgReturnState(s:Int) => devs(sender) = s
    case MsgMove(_, pos, false) => poss += (sender -> pos)
  }

  def AskStatusUpdate() = {
    devs.foreach(dest => dest._1 ! MsgGetState[Int])
  }

  def AskPositionUpdate() = {
    devs.foreach(dest => dest._1 ! MsgGetPosition())
  }

  def DrawAll() = {
    val g = canvas.getGraphics
    // Let's clear then canvas
    g.clearRect(0, 0, 600, 600)
    // Then we draw all the devices
    poss.foreach {  case (ref, pos) => { DrawSingle(g, ref, pos) } }
  }

  def DrawSingle(g: Graphics, ref: ActorRef, pos: Point3D) = {
    g.setColor(Color.BLUE)
    g.fillOval(pos.x.toInt-10, pos.y.toInt-10, 5, 5)
    g.setColor(Color.BLACK)
    //g.drawString(ref.path.name + s" (${pos.x.toInt},${pos.y.toInt})", pos.x.toInt, pos.y.toInt-2)
    g.drawString(""+devs(ref), pos.x.toInt-2, pos.y.toInt+2)
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
