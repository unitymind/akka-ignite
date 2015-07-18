package com.cleawing.ignite.playground

import akka.actor.{Actor, Props}

class TestActor extends Actor {
  def receive = {
    case _ =>
  }
}

object TestActor {
  def apply() : Props = Props[TestActor]
}
