package com.cleawing.ignite.akka

import akka.actor._

import scala.collection.concurrent

object IgniteExtension
  extends ExtensionId[IgniteExtensionImpl]
  with ExtensionIdProvider {

  override def lookup() = IgniteExtension
  override def createExtension(system: ExtendedActorSystem) = {
    systems.put(system.name, system)
    new IgniteExtensionImpl(system)
  }
  override def get(system: ActorSystem): IgniteExtensionImpl = super.get(system)

  // Global (by JVM) registry of ActorSystems
  private[ignite] val systems = concurrent.TrieMap.empty[String, ExtendedActorSystem]

  def resolveActorSystem(name: String) : Option[ExtendedActorSystem] = {
    val systemName = if (name == null) "default" else name
    systems.get(systemName)
  }
}

private[ignite] class IgniteExtensionImpl(val actorSystem: ExtendedActorSystem)
  extends Extension
  with ExtensionAdapter
  with IgniteAdapter {

  protected val gridName : String = actorSystem.name

  // Just entry point into ExtensionAdapter
  init()
}
