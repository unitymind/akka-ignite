package com.cleawing.ignite.akka.transport

import akka.actor._
import com.cleawing.ignite.akka.Ignition
import com.spingo.op_rabbit.consumer.{BoundChannel, Subscription}
import com.spingo.op_rabbit.{QueueMessage, RabbitControl}
import org.apache.ignite.IgniteException
import scala.concurrent.duration._

private[ignite] class RabbitSourceProxyActor(props: Props) extends Actor with Ignition {
  private val services = ignite.Services(ignite.cluster().forRemotes())
  private val selfId = self.path.toStringWithoutAddress
  private val localNodeId = ignite.cluster().localNode().id().toString
  private val deploymentId = s"$localNodeId-$selfId"
  private var rabbitqMq : ActorRef = _
  implicit val kryoFormat = JavaMarshaller
  import context.dispatcher

  lazy val consumeReplies = new Subscription {
    override def config: BoundChannel = channel(1) {
      consume(queue(s"$deploymentId-inbound")) {
        body(as[ProxyEnvelope]) { pe =>
          //            println(s"RabbitSourceProxyActor: $pe")
          context.system.asInstanceOf[ExtendedActorSystem].provider.resolveActorRef(pe.sender) ! pe.message
          ack
        }
      }
    }
  }

  override def preStart(): Unit = {
    if (exists(selfId)) {
      throw InvalidActorNameException(s"actor [$selfId] is already globally deployed")
    }
    try {
      rabbitqMq = context.actorOf(Props[RabbitControl], "rabbitControl")
      services.deployMultiple(selfId, RabbitProxyActorService(props.clazz, props.args, deploymentId), 20, 1)
    } catch {
      case e: IgniteException => throw new IgniteException(s"actor [$selfId] could not remotely deployed due: ${e.getMessage}")
    }

    rabbitqMq ! consumeReplies
  }

  def receive = {
    //TODO. Handle acks
    case msg => rabbitqMq.tell(QueueMessage(ProxyEnvelope(msg, sender(), context.system), queue = s"$deploymentId-outbound"), Actor.noSender)

  }

  override def postStop(): Unit = {
    consumeReplies.close(30.seconds)
    rabbitqMq ! PoisonPill
    services.cancel(selfId)
  }

  private def exists(name: String) : Boolean = services.service[ProxyActorServiceImpl](name) != null
}

object RabbitSourceProxyActor {
  def apply(props: Props) : Props = Props(classOf[RabbitSourceProxyActor], props)
}


