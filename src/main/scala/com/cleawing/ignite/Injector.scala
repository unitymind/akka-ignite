package com.cleawing.ignite

import _root_.akka.actor.{ExtendedActorSystem, ActorRef, ActorSystem}
import com.cleawing.ignite.akka.IgniteGuardian
import com.typesafe.config.{ConfigValueFactory, ConfigFactory, Config}
import scaldi.{TypesafeConfigInjector, Module}
import scaldi.akka.AkkaInjectable._
import scala.collection.JavaConversions._

object Injector {
  implicit val injector = TypesafeConfigInjector() :: new IgniteModule

  def apply() = injector

  def grid: IgniteGrid = inject [IgniteGrid]
  def system : ActorSystem = inject [ActorSystem]
  def ignite : ActorRef = inject [ActorRef]('igniteGuardian)

  private class IgniteModule extends Module {
    bind [IgniteGrid] to IgniteGridFactory(inject [Config]("ignite")) destroyWith { _ =>
      inject [ActorSystem] terminate()
    }
    bind [Config] identifiedBy 'actorSystemConfig to ConfigFactory.load()
      .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(inject [IgniteGrid].cluster().localNode().addresses().last))
      .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(0))
      .withValue("akka.remote.netty.tcp.enabled-transports", ConfigValueFactory.fromIterable(List("akka.remote.netty.tcp")))
      .withValue("akka.actor.provider", ConfigValueFactory.fromAnyRef("akka.remote.RemoteActorRefProvider"))
    // TODO. Use Ignite classLoader for peer2peer class loading?
    bind [ActorSystem] to ActorSystem (inject [String] ("ignite.name"), inject [Config]('actorSystemConfig)) initWith(_.registerOnTermination{
      inject [IgniteGrid] shutdown()
    }) destroyWith(_.terminate())

    bind [ActorRef] as 'igniteGuardian toNonLazy inject [ActorSystem].asInstanceOf[ExtendedActorSystem]
      .systemActorOf(IgniteGuardian(), "ignite")
  }
}
