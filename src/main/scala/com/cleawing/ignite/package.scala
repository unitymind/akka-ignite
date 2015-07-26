package com.cleawing

import akka.actor.{ActorPath, ExtendedActorSystem, ActorRef, ActorSystem}
import com.cleawing.ignite.akka.IgniteGuardian
import com.typesafe.config.{ConfigValueFactory, ConfigFactory, Config}
import org.jetbrains.annotations.Nullable
import scaldi.{Module, TypesafeConfigInjector}
import scaldi.akka.AkkaInjectable._
import scala.annotation.meta.field
import scala.collection.JavaConversions._

package object ignite {
  type NullableField = Nullable @field

  private implicit val injector = TypesafeConfigInjector() :: new IgniteModule

  def init() : Unit = injector.initNonLazy()
  def destroy() : Unit = injector.destroy()

  def grid(): IgniteGrid = inject [IgniteGrid]
  def system() : ActorSystem = inject [ActorSystem]
  def extended() : ExtendedActorSystem = system().asInstanceOf[ExtendedActorSystem]
  def ignite() : ActorRef = inject [ActorRef]('igniteGuardian)
  lazy val rootPath : ActorPath = ActorPath.fromString(extended().provider.getDefaultAddress.toString)

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
