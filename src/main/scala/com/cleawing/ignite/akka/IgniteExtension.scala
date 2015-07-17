package com.cleawing.ignite.akka

import akka.actor._
import scala.collection.concurrent

object IgniteExtension
  extends ExtensionId[IgniteExtensionImpl]
  with ExtensionIdProvider {

  final val PropsCacheName : String = "akka-services-props-cache"

  override def lookup() = IgniteExtension
  override def createExtension(system: ExtendedActorSystem) = new IgniteExtensionImpl(system)
  override def get(system: ActorSystem): IgniteExtensionImpl = super.get(system)

  // Global (by JVM) registry of ActorSystems
  private[ignite] val systems = concurrent.TrieMap.empty[String, ActorSystem]
  private[ignite] val services = concurrent.TrieMap.empty[String, Props]
}

private[ignite] class IgniteExtensionImpl(val system: ExtendedActorSystem)
  extends Extension with ExtensionAdapter {

  init()
}
