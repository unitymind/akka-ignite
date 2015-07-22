package com.cleawing.ignite.playground

import akka.actor.{ActorRef, Props, Actor}
import com.cleawing.ignite.akka.Ignition

class EchoActor2 extends Actor with Ignition {
  private var echo : ActorRef = _
  override def preStart() : Unit = {
    echo = actorOf(EchoActor())
  }

  def receive = {
    case x =>
      if (sender() != echo) {
        echo ! x
        sender ! x
      } else {
        println(x)
      }
  }
}

object EchoActor2 {
  def apply() : Props = Props[EchoActor2]
}

