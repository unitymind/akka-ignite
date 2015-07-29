package com.cleawing.ignite.akka

import akka.actor.{ActorRef, Props, Actor}
import com.cleawing.ignite.akka.remote.RemoteManager
import com.cleawing.ignite.akka.services.ServicesGuardian

class IgniteGuardian extends Actor {
  private var remoteManager : ActorRef = _

  override def preStart(): Unit = {
    remoteManager = context.actorOf(RemoteManager.props(), "remote")
  }

  def receive = {
    case RemoteManager.Started if sender() == remoteManager =>
      context.actorOf(ServicesGuardian.props(), "services")
      context.become(Actor.emptyBehavior)
  }
}

object IgniteGuardian {
  def props() = Props[IgniteGuardian]
}
