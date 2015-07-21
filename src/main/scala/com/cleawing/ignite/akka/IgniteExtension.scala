package com.cleawing.ignite.akka

import akka.actor._
import com.cleawing.ignite.IgniteAdapter
import scala.collection.concurrent

object IgniteExtension extends ExtensionId[IgniteExtensionImpl] with ExtensionIdProvider {
  override def lookup() = IgniteExtension
  override def createExtension(system: ExtendedActorSystem) = {
    systems.put(system.name, system)
    serviceGuardians.put(system.name, system.systemActorOf(ServiceGuardian(), "services"))
    actorGuardians.put(system.name, system.actorOf(ServiceGuardian(), "ignite"))
    new IgniteExtensionImpl(system)
  }
  override def get(system: ActorSystem): IgniteExtensionImpl = super.get(system)

  // Global (by JVM) registries
  private[ignite] val systems = concurrent.TrieMap.empty[String, ExtendedActorSystem]
  private[ignite] val serviceGuardians = concurrent.TrieMap.empty[String, ActorRef]
  private[ignite] val actorGuardians = concurrent.TrieMap.empty[String, ActorRef]

  def resolveActorSystem(name: String) : Option[ExtendedActorSystem] = {
    val systemName = if (name == null) "default" else name
    systems.get(systemName)
  }

  def resolveServiceGuardian(name: String) : Option[ActorRef] = {
    val systemName = if (name == null) "default" else name
    serviceGuardians.get(systemName)
  }

  def resolveActorGuardian(name: String) : Option[ActorRef] = {
    val systemName = if (name == null) "default" else name
    actorGuardians.get(systemName)
  }
}

private[ignite] class IgniteExtensionImpl(val actorSystem: ExtendedActorSystem)
  extends Extension with ExtensionAdapter with IgniteAdapter {

  val name : String = actorSystem.name

  // Just entry point in ExtensionAdapter
  init()
}
