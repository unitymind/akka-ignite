package com.cleawing.ignite.akka.remote

import akka.actor.{PoisonPill, ActorRef, Actor, Props}
import com.cleawing.ignite.akka.Ignition

import scala.collection.mutable.HashMap

class TargetProxyActor(props: Props, deploymentId: String) extends Actor with Ignition {
  import com.cleawing.ignite.akka.remote.TargetProxyActor.ReadMessages

  private val readQueue = ignite.Collection.queue[(Any, String)](s"$deploymentId-writes", 0, null)
  private var target : ActorRef =_
  protected val responders : HashMap[String, ActorRef] = HashMap.empty[String, ActorRef]

  override def preStart() : Unit = {
    target = context.actorOf(props, s"target")
    self ! ReadMessages
  }

  def receive = {
    case ReadMessages =>
      var msg = readQueue.poll()
      while (msg != null) {
        val responder = if (responders.contains(msg._2)) {
          responders(msg._2)
        } else {
          val ref = context.actorOf(ProxyResponderActor(msg._2, deploymentId))
          responders.put(msg._2, ref)
          ref
        }
        target.tell(msg._1, responder)
        msg = readQueue.poll()
      }
      self ! ReadMessages
  }

  override def postStop(): Unit = {
    responders.values.foreach(r => r ! PoisonPill)
    context.stop(target)
  }
}

object TargetProxyActor {
  def apply(props: Props, deploymentId: String) : Props = Props(classOf[TargetProxyActor], props, deploymentId)
  case object ReadMessages
}
