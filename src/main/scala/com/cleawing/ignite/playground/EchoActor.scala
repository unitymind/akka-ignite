package com.cleawing.ignite.playground

import akka.actor.{Actor, Props}

class EchoActor extends Actor {
  def receive = {
    case x => println(x)
  }
}

object EchoActor {
  def apply() : Props = Props[EchoActor]
}
