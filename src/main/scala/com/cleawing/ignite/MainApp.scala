package com.cleawing.ignite

import _root_.akka.actor.ActorSystem
import com.cleawing.ignite.akka.IgniteExtension

object MainApp extends App {
  val system = ActorSystem()
  val ignite = IgniteExtension(system)
}
