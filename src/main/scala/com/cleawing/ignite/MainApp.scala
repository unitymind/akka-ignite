package com.cleawing.ignite

import _root_.akka.actor.ActorSystem
import scaldi.akka.AkkaInjectable._

object MainApp extends App {
  try {
    val system = inject [ActorSystem]
    val igniteGrid = inject [IgniteGrid]
  } catch {
    case t: Throwable =>
      injector.destroy()
      throw t
  }
}
