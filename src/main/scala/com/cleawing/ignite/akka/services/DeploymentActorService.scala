package com.cleawing.ignite.akka.services

import java.util.UUID

import akka.actor.{ActorPath, Props}
import com.cleawing.ignite.akka.IgniteConfig
import org.apache.ignite._
import org.apache.ignite.cache.{CacheAtomicityMode, CacheMemoryMode, CacheRebalanceMode, CacheMode}
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.resources.IgniteInstanceResource
import org.apache.ignite.services.ServiceContext

import com.cleawing.ignite
import org.apache.ignite.transactions.{TransactionIsolation, TransactionConcurrency}

trait DeploymentActorService extends IgniteService

class DeploymentActorServiceImpl(props: Props)
  extends IgniteServiceImpl with DeploymentActorService {
  import DeploymentActorService._

  @IgniteInstanceResource
  @transient
  private var ignite : Ignite = _

  @transient
  private var localDeploymentCache : IgniteCache[ExecutionId, LocalDescriptor] = _

  @transient
  private var deploymentCache : IgniteCache[GlobalDescriptor, NodeId] = _

  @transient
  private var nodeDeploymentSet : IgniteSet[GlobalDescriptor] = _

  private var initRetries = 0

  override def init(ctx: ServiceContext) : Unit = {
    super.init(ctx)
    try {
      localDeploymentCache = ignite.getOrCreateCache[ExecutionId, LocalDescriptor](
        localDeploymentCacheCfg.setName(s"akka_${resolveKind(serviceId)}_services_local")
      ).withAsync()
      deploymentCache = ignite.getOrCreateCache[GlobalDescriptor, NodeId](
        deploymentCacheCfg.setName(s"akka_${resolveKind(serviceId)}_services_deployment")
      ).withAsync()
      nodeDeploymentSet = ignite.set[GlobalDescriptor](s"${ignite.cluster().localNode().id}-deployments",
        IgniteConfig.CollectionBuilder()
          .setCacheMode(CacheMode.PARTITIONED)
          .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
          .setBackups(3)
          .build()
      )
    } catch {
      case t @ (_: IgniteException | _: IgniteCheckedException) =>
        if (initRetries <= 10) {
          initRetries += 1
          Thread.sleep(100)
          init(ctx)
        } else {
          throw t
        }
    }

  }

  override def execute(ctx: ServiceContext) : Unit = {
    val tx = ignite.transactions().txStart()
    try {
      localDeploymentCache.put(executionId, (props, serviceId))
      deploymentCache.put((serviceId, executionId), ignite.cluster().localNode().id)
      nodeDeploymentSet.add((serviceId, executionId))
      tx.commit()
    } catch {
      case t@ (_: IgniteException | _: IgniteCheckedException) =>
        println(t)
        Thread.sleep(100)
        execute(ctx)
    } finally {
      tx.close()
    }
  }

  override def cancel(ctx: ServiceContext) : Unit = {
    val tx = ignite.transactions().txStart()
    try {
      localDeploymentCache.remove(executionId)
      deploymentCache.remove((serviceId, executionId))
      nodeDeploymentSet.remove((serviceId, executionId))
      tx.commit()
    } catch {
      case t@ (_: IgniteException | _: IgniteCheckedException) =>
        Thread.sleep(100)
        execute(ctx)
    } finally {
      tx.close()
    }
  }

  private def resolveKind(serviceId: ServiceId) : String = {
    if (serviceId.contains("@")) "user" else "system"
  }
}

object DeploymentActorService {
  type ExecutionId = UUID
  type NodeId = UUID
  type ServiceId = String
  type LocalDescriptor = (Props, ServiceId)
  type GlobalDescriptor = (ServiceId, ExecutionId)

  val localDeploymentCacheCfg = new CacheConfiguration[ExecutionId, LocalDescriptor]()
    .setCacheMode(CacheMode.LOCAL)
    .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)

  val deploymentCacheCfg = new CacheConfiguration[GlobalDescriptor, NodeId]()
    .setCacheMode(CacheMode.PARTITIONED)
    .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
    .setBackups(3)

  def apply(props: Props) : DeploymentActorServiceImpl = new DeploymentActorServiceImpl(props)
}
