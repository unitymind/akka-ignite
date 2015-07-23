package com.cleawing.ignite

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import org.apache.ignite.IgniteServices
import org.apache.ignite.cache.CacheMemoryMode
import org.apache.ignite.configuration.DeploymentMode
import org.apache.ignite.services.{Service, ServiceConfiguration, ServiceDescriptor}

import scala.collection.JavaConversions._
import scala.concurrent.duration.{Duration, FiniteDuration}

object Implicits {
  final implicit class ConfigOps(val config: Config) extends AnyVal {
    def getCacheMemoryMode(path: String) : CacheMemoryMode = config.getString(path) match {
      case "ONHEAP_TIERED" => CacheMemoryMode.ONHEAP_TIERED
      case "OFFHEAP_TIERED" => CacheMemoryMode.OFFHEAP_TIERED
      case "OFFHEAP_VALUES" => CacheMemoryMode.OFFHEAP_VALUES
      case missed => throw new IllegalArgumentException(s"Only ONHEAP_TIERED, OFFHEAP_TIERED and OFFHEAP_VALUES accepted as cache-memory-mode.")
    }

    def getDeploymentMode(path: String) : DeploymentMode = config.getString(path) match {
      case "PRIVATE" => DeploymentMode.PRIVATE
      case "ISOLATED" => DeploymentMode.ISOLATED
      case "SHARED" => DeploymentMode.SHARED
      case "CONTINUOUS" => DeploymentMode.CONTINUOUS
      case missed => throw new IllegalArgumentException(s"Only PRIVATE, SHARED and CONTINUOUS accepted as deployment-mode.")
    }

    // From akka.utils.Helpers.ConfigOps
    def getMillisDuration(path: String): FiniteDuration = getDuration(path, TimeUnit.MILLISECONDS)
    def getNanosDuration(path: String): FiniteDuration = getDuration(path, TimeUnit.NANOSECONDS)

    private def getDuration(path: String, unit: TimeUnit): FiniteDuration =
      Duration(config.getDuration(path, unit), unit)
  }

  // FIXME. Refactor this kind of implicits
  final implicit class ServicesOps(val svcs: IgniteServices) {
    def deployClusterSingleton(name: String, svc: Service) : Unit = svcs.deployClusterSingleton(name, svc)
    def deployNodeSingleton(name: String, svc: Service) : Unit = svcs.deployNodeSingleton(name, svc)
    def deployKeyAffinitySingleton(name: String, svc: Service, @NullableField cacheName: String, affKey: AnyRef) : Unit = {
      svcs.deployKeyAffinitySingleton(name, svc, cacheName, affKey)
    }
    def deployMultiple(name: String, svc: Service, totalCnt: Int, maxPerNodeCnt: Int) : Unit = {
      svcs.deployMultiple(name, svc, totalCnt, maxPerNodeCnt)
    }
    def deploy(cfg: ServiceConfiguration) : Unit = svcs.deploy(cfg)
    def cancel(name: String) : Unit = svcs.cancel(name)
    def cancelAll() : Unit = svcs.cancelAll()
    def descriptors() : Seq[ServiceDescriptor] = svcs.serviceDescriptors().toSeq
    def proxy[T](name: String, svcItf: Class[T], sticky: Boolean) : T = {
      svcs.serviceProxy[T](name, svcItf, sticky)
    }
  }
}
