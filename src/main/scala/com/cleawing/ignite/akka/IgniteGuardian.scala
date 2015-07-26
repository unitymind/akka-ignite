package com.cleawing.ignite.akka

import akka.actor.{Props, Actor}

class IgniteGuardian extends Actor {
  override def preStart(): Unit = {
    context.actorOf(ServicesGuardian(), "services")
  }

  def receive = Actor.emptyBehavior
}

object IgniteGuardian {
  def apply() = Props[IgniteGuardian]
}
