package com.cleawing.ignite

import _root_.akka.actor.Actor

package object akka {
  trait Ignition { this : Actor =>
    final protected val ignite = IgniteExtension(context.system)
  }
}
