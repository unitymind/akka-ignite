package com.cleawing.ignite.akka

import akka.actor.ActorSystem
import org.apache.ignite.Ignite
import org.apache.ignite.resources.IgniteInstanceResource
import org.apache.ignite.services.{Service, ServiceContext}

trait IgniteService extends Service {
  @IgniteInstanceResource
  protected var _ignite: Ignite = null

  def cancel(ctx: ServiceContext) : Unit = ()
  def init(ctx: ServiceContext) : Unit = ()
  def execute(ctx: ServiceContext) : Unit = ()

  // TODO. How to block destructive methods on ActorSystem (e.g. terminate()) ?
  final protected def system : ActorSystem          = IgniteExtension.resolveActorSystem(_ignite.name())
  final protected def ignite : IgniteExtensionImpl  = IgniteExtension(system)
}


