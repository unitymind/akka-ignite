package com.cleawing.ignite

import _root_.akka.actor.ActorSystem
import com.typesafe.config.Config
import scaldi.Module

object Injector {
  class IgniteModule extends Module {
    bind [ActorSystem] to ActorSystem(inject [String] ("ignite.name")) destroyWith(_.shutdown())
    bind [IgniteGrid] to IgniteGridFactory(inject [Config] ("ignite")) destroyWith(_.shutdown())
  }
}
