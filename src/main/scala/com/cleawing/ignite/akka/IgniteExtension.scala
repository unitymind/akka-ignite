package com.cleawing.ignite.akka

import java.util.concurrent.ExecutorService

import akka.actor._
import com.cleawing.ignite.NullableField
import org.apache.ignite.cache.affinity.Affinity
import org.apache.ignite.cluster.ClusterGroup
import org.apache.ignite.configuration.{CollectionConfiguration, NearCacheConfiguration, IgniteConfiguration, CacheConfiguration}
import org.apache.ignite.internal.IgnitionEx
import org.apache.ignite._
import org.apache.ignite.lang.IgniteProductVersion
import org.apache.ignite.plugin.IgnitePlugin

object IgniteExtension
  extends ExtensionId[IgniteExtensionImpl]
  with ExtensionIdProvider {

  override def lookup() = IgniteExtension
  override def createExtension(system: ExtendedActorSystem) = new IgniteExtensionImpl(system)
  override def get(system: ActorSystem): IgniteExtensionImpl = super.get(system)
}

private[ignite] class IgniteExtensionImpl(val system: ExtendedActorSystem)
  extends Extension {
  import scala.collection.JavaConversions.iterableAsScalaIterable

  init()

  def name : String = ignite().name()
  def log() : IgniteLogger = ignite().log()
  def configuration() : IgniteConfiguration = ignite().configuration()
  def cluster() : IgniteCluster = ignite().cluster()
  def compute() : IgniteCompute = ignite().compute()
  def compute(grp: ClusterGroup) : IgniteCompute = ignite().compute(grp)
  def messages() : IgniteMessaging = ignite().message()
  def messages(grp: ClusterGroup) : IgniteMessaging = ignite().message(grp)
  def events() : IgniteEvents = ignite().events()
  def events(grp: ClusterGroup) : IgniteEvents = ignite().events(grp: ClusterGroup)
  def services() : IgniteServices = ignite().services()
  def services(grp: ClusterGroup) : IgniteServices = ignite().services(grp)
  def executorService() : ExecutorService = ignite().executorService()
  def executorService(grp: ClusterGroup) : ExecutorService = ignite().executorService(grp)
  def version() : IgniteProductVersion = ignite().version()
  def scheduler() : IgniteScheduler = ignite().scheduler()

  object cache {
    def config[K, V] : CacheConfiguration[K, V] = new CacheConfiguration()
    def addConfig[K, V](cacheCfg: CacheConfiguration[K, V]) = ignite().addCacheConfiguration(cacheCfg)
    def create[K, V](cacheCfg: CacheConfiguration[K, V]) : IgniteCache[K, V] = ignite().createCache(cacheCfg)
    def create[K, V](cacheCfg: CacheConfiguration[K, V], nearCfg: NearCacheConfiguration[K, V]) : IgniteCache[K, V] = {
      ignite().createCache(cacheCfg, nearCfg)
    }
    def create[K, V](cacheName: String) : IgniteCache[K, V] = ignite().createCache[K, V](cacheName)
    def getOrCreate[K, V](cacheCfg: CacheConfiguration[K, V]) : IgniteCache[K, V] = ignite().getOrCreateCache(cacheCfg)
    def getOrCreate[K, V](cacheCfg: CacheConfiguration[K, V], nearCfg: NearCacheConfiguration[K, V]) : IgniteCache[K, V] = {
      ignite().getOrCreateCache(cacheCfg, nearCfg)
    }
    def getOrCreate[K, V](cacheName: String) : IgniteCache[K, V] = ignite().getOrCreateCache[K, V](cacheName)
    def createNear[K, V](@NullableField cacheName: String, nearCfg: NearCacheConfiguration[K, V]) : IgniteCache[K, V] = {
      ignite().createNearCache(cacheName, nearCfg)
    }
    def getOrCreateNear[K, V](@NullableField cacheName: String, nearCfg: NearCacheConfiguration[K, V]) : IgniteCache[K, V] = {
      ignite().createNearCache(cacheName, nearCfg)
    }
    def destroy(cacheName: String) : Unit = ignite().destroyCache(cacheName)
    def apply[K, V](@NullableField name: String) : IgniteCache[K, V] = ignite().cache(name)
  }

  def transactions() : IgniteTransactions = ignite().transactions()
  def dataStreamer[K, V](@NullableField cacheName: String) : IgniteDataStreamer[K, V] = ignite().dataStreamer(cacheName)
  def fileSystem(name: String) : IgniteFileSystem = ignite().fileSystem(name)
  def fileSystems() : Iterable[IgniteFileSystem] = ignite().fileSystems()
  def atomicSequence(name: String, initVal: Long, create: Boolean) : IgniteAtomicSequence = ignite().atomicSequence(name, initVal, create)
  def atomicLong(name: String, initVal: Long, create: Boolean) : IgniteAtomicLong = ignite().atomicLong(name, initVal, create)
  def atomicReference[T](name: String, @NullableField initVal: T, create: Boolean) : IgniteAtomicReference[T] = {
    ignite().atomicReference(name, initVal, create)
  }
  def atomicStamped[T, S](name: String, @NullableField initVal: T, @NullableField initStamp: S, create: Boolean) : IgniteAtomicStamped[T, S] = {
    ignite().atomicStamped(name, initVal, initStamp, create)
  }
  def countDownLatch(name: String, cnt: Int, autoDel: Boolean, create: Boolean) : IgniteCountDownLatch = {
    ignite().countDownLatch(name, cnt, autoDel, create)
  }
  def queue[T](name: String, cap: Int, @NullableField cfg : CollectionConfiguration) : IgniteQueue[T] = ignite().queue(name, cap, cfg)
  def set[T](name: String, @NullableField cfg : CollectionConfiguration) : IgniteSet[T] = ignite().set(name, cfg)
  def plugin[T <: IgnitePlugin](name: String) : T = ignite().plugin(name)
  def affinity[K](cacheName: String) : Affinity[K] = ignite().affinity(cacheName)

  private def ignite() : Ignite = Ignition.ignite(system.name)

  private def init() : Unit = {
    start(system.name)
    system.registerOnTermination(stop(system.name))
  }

  // TODO. Implement idiomatic TypeSafe akka config (and do not depend on Spring Beans)
  private def start(name: String) : Unit = {
    IgnitionEx.start(getClass.getResourceAsStream("/reference_ignite.xml"), name, null)
  }

  private def stop(name: String): Unit = {
    Ignition.stop(name, false)
  }
}
