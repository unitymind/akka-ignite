package com.cleawing.ignite.akka

import akka.actor.{ActorRef, ExtendedActorSystem}
import org.apache.ignite.Ignite
import org.apache.ignite.resources.IgniteInstanceResource
import org.apache.ignite.services.{Service, ServiceContext}

trait IgniteService extends Service {
  @IgniteInstanceResource
  protected var _ignite: Ignite = null
  protected var _guardian : ActorRef = null

  def cancel(ctx: ServiceContext) : Unit = ()
  def init(ctx: ServiceContext) : Unit = ()
  def execute(ctx: ServiceContext) : Unit = ()

  // TODO. How to block destructive methods on ActorSystem (e.g. terminate()) ?
  final protected def system : ExtendedActorSystem = IgniteExtension.resolveActorSystem(_ignite.name()).get
  final protected def ignite : IgniteExtensionImpl  = IgniteExtension(system)
}


