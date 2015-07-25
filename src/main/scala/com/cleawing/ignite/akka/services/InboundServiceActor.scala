package com.cleawing.ignite.akka.services

import java.util.UUID
import akka.actor.{Props, Actor}
import com.cleawing.ignite.Injector

import scala.concurrent.Future

class InboundServiceActor(serviceName: String, target: String, targetNodeId: UUID) extends Actor {
  import context.dispatcher

  def receive =  {
    case reply => Future { proxy.reply(ProxyEnvelope(reply, target, targetNodeId)) }
  }

  private val proxy = Injector.grid.Services()
    .serviceProxy[ActorServiceCollector](s"$serviceName-$targetNodeId", classOf[ActorServiceCollector], false)
}

object InboundServiceActor {
  def apply(serviceName: String, target: String, targetNodeId: UUID) : Props =
    Props(classOf[InboundServiceActor], serviceName, target, targetNodeId)
}
