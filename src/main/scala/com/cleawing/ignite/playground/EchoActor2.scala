package com.cleawing.ignite.playground

import akka.actor.{ActorRef, Props, Actor}
import com.cleawing.ignite.akka.Ignition

class EchoActor2 extends Actor {
  import com.cleawing.ignite.Implicits.ActorSystemOps

  private var echo : ActorRef = _
  override def preStart() : Unit = {
    echo = context.system.serviceOf(EchoActor(), "echo2", 0, 1)
  }

  def receive = {
    case x =>
      if (sender() != echo) {
        echo ! x
//        sender ! x
      } else {
        println(x)
      }
  }

  override def postStop() : Unit  = context.system.stop(echo)
}

object EchoActor2 {
  def apply() : Props = Props[EchoActor2]
}

