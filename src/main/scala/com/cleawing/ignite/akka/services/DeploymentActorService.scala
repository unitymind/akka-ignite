package com.cleawing.ignite.akka.services

import java.util.UUID

import akka.actor.{ActorPath, Props}
import org.apache.ignite.{Ignite, IgniteCache}
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.resources.IgniteInstanceResource
import org.apache.ignite.services.ServiceContext

import com.cleawing.ignite

trait DeploymentActorService extends IgniteService

class DeploymentActorServiceImpl(props: Props, parent: Option[String])
  extends IgniteServiceImpl with DeploymentActorService {
  import DeploymentActorService._

  @transient private var localCache : IgniteCache[UUID, Descriptor] = _
  @IgniteInstanceResource private var ignite : Ignite = _

  override def init(ctx: ServiceContext) : Unit = {
    super.init(ctx)
    localCache = ignite.getOrCreateCache[UUID, Descriptor](DeploymentActorService.localCacheCfg.setName(s"akka_${resolveKind(parent)}_services"))
  }

  override def execute(ctx: ServiceContext) : Unit = {
    localCache.put(executionId, (props, name, parent))
  }

  override def cancel(ctx: ServiceContext) : Unit = {
    localCache.remove(executionId)
  }


  private def resolveKind(parent: Option[String]) : String = {
    if (parent.isEmpty) "global" else "user"
  }
}

object DeploymentActorService {
  type Descriptor = (Props, String, Option[String])
  val localCacheCfg = new CacheConfiguration[UUID, Descriptor]().setCacheMode(CacheMode.LOCAL)
  def apply(props: Props, parent: Option[String]) : DeploymentActorServiceImpl = new DeploymentActorServiceImpl(props, parent)
}
