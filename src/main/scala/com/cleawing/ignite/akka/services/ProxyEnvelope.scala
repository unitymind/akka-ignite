package com.cleawing.ignite.akka.services

import java.util.UUID

import akka.actor.{Actor, ActorSystem, ActorRef}


final case class ProxyEnvelope (message: Any, sender: String, nodeId: UUID)

object ProxyEnvelope {
  def apply(message: Any, sender: ActorRef, system: ActorSystem, nodeId: UUID): ProxyEnvelope = {
    if (message == null) throw new IllegalArgumentException("Message is null")
    new ProxyEnvelope(message, (if (sender ne Actor.noSender) sender else system.deadLetters).path.toSerializationFormat, nodeId)
  }
}

