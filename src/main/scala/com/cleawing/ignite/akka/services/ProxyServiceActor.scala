package com.cleawing.ignite.akka.services

import akka.actor.{Props, Actor}
import com.cleawing.ignite.Injector

import scala.concurrent.Future

class ProxyServiceActor extends Actor {
  import context.dispatcher

  private implicit val grid = Injector.grid

  private val serviceName = isSystem match {
    case true => s"${self.path.toStringWithoutAddress}"
    case false => s"${self.path.toSerializationFormat.replace(context.system.toString, "")}"
  }

  private val localNodeId = grid.cluster().localNode().id()
  private val proxy = grid.Services().serviceProxy[ActorService](serviceName, classOf[ActorService], false)
  private def isSystem = self.path.elements.head == "system"

  def receive = {
    case message =>
      val source = sender()
      Future {
        proxy.tell(ProxyEnvelope(message, source, context.system, localNodeId))
    }
  }

  override def postStop() : Unit = {
    if (!isSystem) {
      grid.Services().cancel(serviceName)
      grid.Services().cancel(s"$serviceName-$localNodeId")
    }
  }
}

object ProxyServiceActor {
  def apply() : Props = Props[ProxyServiceActor]
}
