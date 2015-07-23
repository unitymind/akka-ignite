package com.cleawing.ignite.playground

import akka.actor.{Actor, Props}
import com.cleawing.ignite.akka.Ignition
import org.apache.ignite.IgniteQueue


private[ignite] class ProxyResponderActor(sender: String, deploymentId: String) extends Actor with Ignition {
  private val writeQueue : IgniteQueue[(Any, String)] = ignite.Collection.queue[(Any, String)](s"$deploymentId-reads", 0, null)

  def receive = {
    case msg => writeQueue.add((msg, sender))
  }
}

private[ignite] object ProxyResponderActor {
  def apply(sender: String, deploymentId: String) : Props = Props(classOf[ProxyResponderActor], sender, deploymentId)
}
