package com.cleawing.ignite.playground

import akka.actor.{ActorRef, Props, Actor}
import com.cleawing.ignite.akka.Ignition
import com.cleawing.ignite.akka.services.ServiceSelection

class IgnitionEchoActor extends Actor with Ignition {
  var echo : ActorRef = _
  override def preStart(): Unit = {
    echo = context.serviceOf(SimpleEchoActor(), "echo", 0, 1)
  }

  def receive = {
    case x =>
      echo.forward(x)
  }
}

object IgnitionEchoActor {
  def apply() : Props = Props[IgnitionEchoActor]
}

