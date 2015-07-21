package com.cleawing.ignite.akka.remote

import akka.actor.{Actor, ActorSystem, ActorRef}

final case class ProxyEnvelope (message: Any, sender: String, deploymentName: String)

object ProxyEnvelope {
  def apply(message: Any, sender: ActorRef, deploymentName: String, system: ActorSystem) = {
    if (message == null) throw new IllegalArgumentException("Message is null")
    val s = if (sender ne Actor.noSender) sender else system.deadLetters
    new ProxyEnvelope(message, s.path.toSerializationFormat, deploymentName)
  }
}