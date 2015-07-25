package com.cleawing.ignite

import _root_.akka.actor.ActorSystem
import com.typesafe.config.{ConfigValueFactory, ConfigFactory, Config}
import scaldi.Module
import scaldi.akka.AkkaInjectable._
import scala.collection.JavaConversions._

object Injector {
  def actorSystem() : ActorSystem = inject [ActorSystem]
  def grid(): IgniteGrid = inject [IgniteGrid]

  class IgniteModule extends Module {
    bind [IgniteGrid] to IgniteGridFactory(inject[Config]("ignite"))
    bind [String] identifiedBy 'bindingHost to inject[IgniteGrid].cluster().localNode().addresses().last
    // TODO. Use Ignite classLoader for peer2peer class loading?
    bind [ActorSystem] to ActorSystem (
      inject [String] ("ignite.name"),
      ConfigFactory.load()
        .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(inject [String]('bindingHost)))
        .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(0))
        .withValue("akka.remote.netty.tcp.enabled-transports", ConfigValueFactory.fromIterable(List("akka.remote.netty.tcp")))
        .withValue("akka.actor.provider", ConfigValueFactory.fromAnyRef("akka.remote.RemoteActorRefProvider"))
    )
  }
}
