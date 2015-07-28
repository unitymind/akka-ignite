package com.cleawing.ignite.playground

import akka.actor.{ActorRef, Props, Actor}
import com.cleawing.ignite.akka.Ignition

class IgnitionEchoActor extends Actor with Ignition {
  var echo : ActorRef = _
  override def preStart(): Unit = {
    echo = context.serviceOf(SimpleEchoActor(), "echo", 100, 0)
  }

  def receive = {
    case x =>
      echo.forward(x)
  }
}

object IgnitionEchoActor {
  def apply() : Props = Props[IgnitionEchoActor]
}

