package com.cleawing.ignite.akka.services

import akka.actor.{Props, Actor}

class ServiceProxyActor extends Actor {
  def receive = Actor.emptyBehavior
}

object ServiceProxyActor {
  def apply() : Props = Props[ServiceProxyActor]
}
