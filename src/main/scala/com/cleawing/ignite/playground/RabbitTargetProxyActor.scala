package com.cleawing.ignite.playground

import akka.actor._
import com.spingo.op_rabbit.RabbitControl
import com.spingo.op_rabbit.consumer.{BoundChannel, Subscription}

import scala.collection.mutable.HashMap
import scala.concurrent.duration._

class RabbitTargetProxyActor(props: Props, deploymentId: String) extends Actor {

  private var target : ActorRef =_
  protected val responders : HashMap[String, ActorRef] = HashMap.empty[String, ActorRef]
  private var rabbitqMq : ActorRef = _
  implicit val javaMarshaller = JavaMarshaller
  import context.dispatcher

  lazy val consumeMessage = new Subscription {
    override def config: BoundChannel = channel(1) {
      consume(queue(s"$deploymentId-outbound")) {
        body(as[ProxyEnvelope]) { pe =>
          //            println(s"RabbitTargetProxyActor: $pe")
          val responder = if (responders.contains(pe.sender)) {
            responders(pe.sender)
          } else {
            val ref = context.actorOf(RabbitProxyResponderActor(pe.sender, deploymentId, rabbitqMq, target))
            responders.put(pe.sender, ref)
            ref
          }
          target.tell(pe.message, responder)
          ack
        }
      }
    }
  }

  override def preStart() : Unit = {
    target = context.actorOf(props, s"target")
    rabbitqMq = context.actorOf(Props[RabbitControl], "rabbitControl")
    rabbitqMq ! consumeMessage
  }

  def receive = {
    case _ =>
  }

  override def postStop(): Unit = {
    consumeMessage.close(30.seconds)
    responders.values.foreach(r => r ! PoisonPill)
    context.stop(target)
    context.stop(rabbitqMq)
  }
}

object RabbitTargetProxyActor {
  def apply(props: Props, deploymentId: String) : Props = Props(classOf[RabbitTargetProxyActor], props, deploymentId)
  case object ReadMessages
}


