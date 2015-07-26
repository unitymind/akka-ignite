package com.cleawing.ignite.playground

import akka.actor.{Props, Actor}

class SimpleEchoActor extends Actor {
  def receive = {
    case x =>
      println(x)
      sender() ! x
  }
}

object SimpleEchoActor {
  def apply() : Props = Props[SimpleEchoActor]
}