package com.cleawing.ignite.playground

import akka.actor.{ActorSelection, Props, Actor}
import com.cleawing.ignite.akka.Ignition

class IgnitionEchoActor extends Actor with Ignition {

  var echo : ActorSelection = _
  override def preStart(): Unit = {
    echo = context.serviceOf(SimpleEchoActor(), "echo", 50, 0)
    println(echo)
  }

  def receive = {
    case x =>
      echo forward x
  }
}

object IgnitionEchoActor {
  def apply() : Props = Props[IgnitionEchoActor]
}

