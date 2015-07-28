package com.cleawing.ignite.akka

import akka.actor.{Props, Actor}
import com.cleawing.ignite.akka.remote.RemoteManager
import com.cleawing.ignite.akka.services.ServicesGuardian

class IgniteGuardian extends Actor {
  override def preStart(): Unit = {
    context.actorOf(RemoteManager.props(), "remote")
    context.actorOf(ServicesGuardian(), "services")
  }

  def receive = Actor.emptyBehavior
}

object IgniteGuardian {
  def apply() = Props[IgniteGuardian]
}
