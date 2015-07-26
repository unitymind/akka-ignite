package com.cleawing.ignite.akka.services

import akka.actor.{Props, Actor}

class GlobalGuardian extends Actor {
  def receive = Actor.emptyBehavior
}

object GlobalGuardian {
  def apply() : Props = Props[GlobalGuardian]
}
