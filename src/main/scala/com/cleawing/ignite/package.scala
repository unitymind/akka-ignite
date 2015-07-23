package com.cleawing

import org.jetbrains.annotations.Nullable

import scala.annotation.meta.field
import _root_.akka.actor.ActorSystem
import scaldi.{TypesafeConfigInjector, Module}

package object ignite {
  type NullableField = Nullable @field
  import scaldi.akka.AkkaInjectable._

  implicit val injector = TypesafeConfigInjector() :: new IgniteModule
  implicit val actorSystem = inject [ActorSystem]

  private class IgniteModule extends Module {
    bind [ActorSystem] to ActorSystem(inject [String] ("ignite.name"))
  }
}
