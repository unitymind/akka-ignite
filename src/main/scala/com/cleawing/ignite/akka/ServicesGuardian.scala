package com.cleawing.ignite.akka

import akka.actor.{Props, Actor}
import com.cleawing.ignite.akka.services.UserGuardian

class ServicesGuardian extends Actor {
  private val userGuardian = context.actorOf(UserGuardian(), "user")
  
  def receive = Actor.emptyBehavior
}

object ServicesGuardian {
  def apply() : Props = Props[ServicesGuardian]
}
