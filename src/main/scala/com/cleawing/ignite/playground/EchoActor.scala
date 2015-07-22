package com.cleawing.ignite.playground

import akka.actor.{Props, Actor}

class EchoActor extends Actor {
  def receive = {
    case x =>
      println(x)
//      println(sender())
      sender() ! x
  }
}

object EchoActor {
  def apply() : Props = Props[EchoActor]
}