package com.cleawing.ignite.akka.remote

import akka.actor.{Props, Actor}
import com.cleawing.ignite.akka.Ignition
import org.apache.ignite.IgniteQueue


private[ignite] class IgniteProxyResponderActor(target: String, deploymentName: String) extends Actor with Ignition {
  private var queue : IgniteQueue[(String, Any)] = _

  override def preStart() : Unit = {
   queue = ignite.Collection.queue[(String, Any)](deploymentName, 0, null)
  }

  def receive = {
    case msg => queue.add((target, msg))
  }
}

private[ignite] object IgniteProxyResponderActor {
  def apply(target: String, deploymentName: String) : Props = Props(classOf[IgniteProxyResponderActor], target, deploymentName)
}
