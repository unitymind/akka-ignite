package com.cleawing.ignite.playground

import akka.actor.{Actor, ActorRef, Props}
import com.cleawing.ignite.akka.Ignition
import com.spingo.op_rabbit.QueueMessage


private[ignite] class RabbitProxyResponderActor(source: String, deploymentId: String, rabbitMq: ActorRef, target: ActorRef) extends Actor with Ignition {
  implicit val kryoFormat = JavaMarshaller

  def receive = {
    //TODO. Handle acks
    case msg =>
      rabbitMq.tell(QueueMessage(ProxyEnvelope(msg, source), queue = s"$deploymentId-inbound"), Actor.noSender)
  }
}

private[ignite] object RabbitProxyResponderActor {
  def apply(source: String, deploymentId: String, rabbitMq: ActorRef, target: ActorRef) : Props = {
    Props(classOf[RabbitProxyResponderActor], source, deploymentId, rabbitMq, target)
  }
}


