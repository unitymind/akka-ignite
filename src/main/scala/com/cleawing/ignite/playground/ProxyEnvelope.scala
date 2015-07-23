package com.cleawing.ignite.playground

import akka.actor.{Actor, ActorRef, ActorSystem}

final case class ProxyEnvelope (message: Any, sender: String)

object ProxyEnvelope {
  def apply(message: Any, sender: ActorRef, system: ActorSystem): ProxyEnvelope = {
    if (message == null) throw new IllegalArgumentException("Message is null")
    new ProxyEnvelope(message, (if (sender ne Actor.noSender) sender else system.deadLetters).path.toSerializationFormat)
  }
}
