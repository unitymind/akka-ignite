package com.cleawing.ignite

import java.util.concurrent.TimeUnit

import _root_.akka.actor.{ActorRef, Props, ActorSystem}
import com.cleawing.ignite.akka.services.{ActorServiceCollector, OutboundServiceActor, ActorService}
import com.typesafe.config.Config
import org.apache.ignite.IgniteServices
import org.apache.ignite.cache.CacheMemoryMode
import org.apache.ignite.configuration.DeploymentMode
import org.apache.ignite.services.{Service, ServiceConfiguration, ServiceDescriptor}
import scaldi.Injectable

import scala.collection.JavaConversions._
import scala.concurrent.duration.{Duration, FiniteDuration}

object Implicits {
  final implicit class ActorSystemOps(val system: ActorSystem) extends Injectable {
    private val grid = inject [IgniteGrid]
    private def remoteServices() = grid.Services(grid.cluster().forRemotes()).withAsync()
    private def localServices() = grid.Services(grid.cluster().forLocal()).withAsync()

    def serviceOf(props: Props, name: String, totalCnt: Int, maxPerNodeCnt: Int) : ActorRef = {
      val ref = system.actorOf(OutboundServiceActor(), name)
      val serviceName = s"${ref.path.toSerializationFormat.replace(system.toString, "")}"
      remoteServices()
        .deployMultiple(serviceName, ActorService(props.clazz, props.args:_*), totalCnt, maxPerNodeCnt)

      localServices()
        .deployClusterSingleton(s"$serviceName-${grid.cluster().localNode().id}", ActorServiceCollector(ref.path.toSerializationFormat))

      ref
    }

    def serviceStop(name: String) : Unit = {
      remoteServices().cancel(name)
    }
  }

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
