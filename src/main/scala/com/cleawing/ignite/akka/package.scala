package com.cleawing.ignite

import _root_.akka.actor.{ActorRef, Actor}

package object akka {
  trait Ignition extends { this: Actor =>
    val ignite : ActorRef = com.cleawing.ignite.ignite()
  }

  object Ignition {

  }
}
