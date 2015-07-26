package com.cleawing.ignite.akka.services

import java.util.UUID

import akka.actor.{ActorRef, ActorPath, Props}
import org.apache.ignite.IgniteCache
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.services.ServiceContext

import com.cleawing.ignite

trait ActorService extends IgniteService {
  def path() : ActorPath
}

class ActorServiceImpl(props: Props, parent: Option[String])
  extends IgniteServiceImpl with ActorService {

  private var cache : IgniteCache[UUID, ActorService.ServiceDescriptor] = _

  override def init(ctx: ServiceContext) : Unit = {
    super.init(ctx)
    cache = ignite.grid().Cache
      .getOrCreate[UUID, ActorService.ServiceDescriptor](ActorService.cacheCfg.setName(s"akka_${resolveType(parent)}_services"))
  }

  override def execute(ctx: ServiceContext) : Unit = {
    cache.put(executionId, (props, name, parent))
  }

  override def cancel(ctx: ServiceContext) : Unit = {
    cache.remove(executionId)
  }

  def path() : ActorPath = {
    ignite.rootPath() / "system" / "ignite" / "services" / resolveType(parent) / executionId.toString
  }

  private def resolveType(parent: Option[String]) : String = {
    if (parent.isEmpty) "global" else "user"
  }
}

object ActorService {
  type ServiceDescriptor = (Props, String, Option[String])
  val cacheCfg = ignite.grid().Cache.config[UUID, ServiceDescriptor]().setCacheMode(CacheMode.LOCAL)
  def apply(props: Props, parent: Option[String]) : ActorServiceImpl = new ActorServiceImpl(props, parent)
}
