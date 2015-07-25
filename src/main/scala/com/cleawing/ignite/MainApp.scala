package com.cleawing.ignite

import _root_.akka.actor.ActorSystem
import scaldi.akka.AkkaInjectable._

object MainApp extends App {
  try {
    val igniteGrid = inject [IgniteGrid]
    val system = inject [ActorSystem]
  } catch {
    case t: Throwable =>
      injector.destroy()
      throw t
  }
}
