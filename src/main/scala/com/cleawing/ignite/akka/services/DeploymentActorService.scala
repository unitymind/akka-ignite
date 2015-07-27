package com.cleawing.ignite.akka.services

import java.util.UUID

import akka.actor.{ActorPath, Props}
import org.apache.ignite.IgniteCache
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.services.ServiceContext

import com.cleawing.ignite

trait DeploymentActorService extends IgniteService {
  def path() : ActorPath
}

class DeploymentActorServiceImpl(props: Props, parent: Option[String])
  extends IgniteServiceImpl with DeploymentActorService {
  import DeploymentActorService._

  @transient private var localCache : IgniteCache[UUID, Descriptor] = _

  override def init(ctx: ServiceContext) : Unit = {
    super.init(ctx)
    localCache = ignite.grid().Cache
      .getOrCreate[UUID, Descriptor](DeploymentActorService.localCacheCfg.setName(s"akka_${resolveKind(parent)}_services"))

  }

  override def execute(ctx: ServiceContext) : Unit = {
    localCache.put(executionId, (props, name, parent))
  }

  override def cancel(ctx: ServiceContext) : Unit = {
    localCache.remove(executionId)
  }

  def path() : ActorPath = {
    ignite.rootPath / "system" / "ignite" / "services" / resolveKind(parent) / executionId.toString
  }

  private def resolveKind(parent: Option[String]) : String = {
    if (parent.isEmpty) "global" else "user"
  }
}

object DeploymentActorService {
  type Descriptor = (Props, String, Option[String])
  val localCacheCfg = ignite.grid().Cache.config[UUID, Descriptor]().setCacheMode(CacheMode.LOCAL)
  def apply(props: Props, parent: Option[String]) : DeploymentActorServiceImpl = new DeploymentActorServiceImpl(props, parent)
}
