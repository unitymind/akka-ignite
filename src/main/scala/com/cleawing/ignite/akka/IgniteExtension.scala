package com.cleawing.ignite.akka

import akka.actor._
import org.apache.ignite.{Ignite, Ignition}

object IgniteExtension
  extends ExtensionId[IgniteExtensionImpl]
  with ExtensionIdProvider {

  override def lookup() = IgniteExtension
  override def createExtension(system: ExtendedActorSystem) = new IgniteExtensionImpl(system)
  override def get(system: ActorSystem): IgniteExtensionImpl = super.get(system)

}

private[ignite] class IgniteExtensionImpl(val system: ExtendedActorSystem)
  extends Extension {

  def ignite() : Ignite = Ignition.ignite()
}
