package com.cleawing.ignite.akka

import akka.actor._

object IgniteExtension
  extends ExtensionId[IgniteExtensionImpl]
  with ExtensionIdProvider {

  override def lookup() = IgniteExtension
  override def createExtension(system: ExtendedActorSystem) = new IgniteExtensionImpl(system)
  override def get(system: ActorSystem): IgniteExtensionImpl = super.get(system)
}

private[ignite] class IgniteExtensionImpl(val system: ExtendedActorSystem)
  extends Extension with ExtensionAdapter {

  init()
}
