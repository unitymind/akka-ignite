package com.cleawing.ignite.akka

import akka.actor.{ActorRef, Props, ActorSystem}
import org.apache.ignite.Ignite
import org.apache.ignite.resources.IgniteInstanceResource
import org.apache.ignite.services.{ServiceContext, Service}

import scala.beans.BeanProperty

trait IgniteService extends Service {
  @IgniteInstanceResource
  @BeanProperty var internalIgnite: Ignite = null

  protected var serviceActor : Option[ActorRef] = None

  def init(ctx: ServiceContext) : Unit = {
    // Will fail, if Actor Props (e.g. service entry point) is not resolved.
    // It's expected behaviour
    serviceActor = Some(actorSystem.actorOf(resolveProps(ctx.name()), ctx.executionId().toString))
  }

  def execute(cxt: ServiceContext) : Unit = {
    // override in your service implementation,
    // for instance - sending initial message to serviceActor
  }

  def cancel(ctx: ServiceContext) : Unit = {
    actorSystem.stop(serviceActor.get)
  }

  // TODO. How to block destructive methods on ActorSystem (e.g. terminate())
  final protected def actorSystem : ActorSystem     = IgniteExtension.systems(internalIgnite.name())
  final protected def ignite : IgniteExtensionImpl  = IgniteExtension(actorSystem)

  final protected def resolveProps(serviceName: String): Props = {
    ignite.Cache[String, Props](IgniteExtension.PropsCacheName).get(serviceName)
  }
}
