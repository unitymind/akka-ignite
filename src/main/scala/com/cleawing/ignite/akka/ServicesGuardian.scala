package com.cleawing.ignite.akka

import akka.actor.{Props, Actor}

class ServicesGuardian extends Actor {
  def receive = Actor.emptyBehavior
}

object ServicesGuardian {
  def apply() : Props = Props[ServicesGuardian]
}
