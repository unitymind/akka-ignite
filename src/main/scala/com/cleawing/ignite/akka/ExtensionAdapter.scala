package com.cleawing.ignite.akka

import java.util.concurrent.ExecutorService

import _root_.akka.actor.ExtendedActorSystem
import org.apache.ignite.cache.affinity.Affinity
import org.apache.ignite.cluster.ClusterGroup
import org.apache.ignite._
import org.apache.ignite.configuration._
import org.apache.ignite.internal.IgnitionEx
import org.apache.ignite.lang.IgniteProductVersion
import org.apache.ignite.plugin.IgnitePlugin


private[ignite] trait ExtensionAdapter {
  import com.cleawing.ignite._
  import scala.collection.JavaConversions.iterableAsScalaIterable

  def system: ExtendedActorSystem

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

  object Cache {
    def config[K, V]() : CacheConfiguration[K, V] = new CacheConfiguration()
    def config[K, V](name: String) : CacheConfiguration[K, V] = new CacheConfiguration(name)
    def config[K, V](cfg: CacheConfiguration[K, V]) : CacheConfiguration[K, V] = new CacheConfiguration(cfg)
    def configNear[K, V] : NearCacheConfiguration[K, V] = new NearCacheConfiguration()
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

  object Transactions {
    def apply() : IgniteTransactions = ignite().transactions()
    def config() : TransactionConfiguration = new TransactionConfiguration()
    def config(cfg: TransactionConfiguration) : TransactionConfiguration = new TransactionConfiguration(cfg)
  }

  def dataStreamer[K, V](@NullableField cacheName: String) : IgniteDataStreamer[K, V] = ignite().dataStreamer(cacheName)

  object IGFS {
    def apply() : Iterable[IgniteFileSystem] = ignite().fileSystems()
    def apply(name: String) : IgniteFileSystem = ignite().fileSystem(name)
    def config() : FileSystemConfiguration = new FileSystemConfiguration()
    def config(cfg: FileSystemConfiguration) = new FileSystemConfiguration(cfg)
  }

  object Atomic {
    def sequence(name: String, initVal: Long, create: Boolean) : IgniteAtomicSequence = ignite().atomicSequence(name, initVal, create)
    def long(name: String, initVal: Long, create: Boolean) : IgniteAtomicLong = ignite().atomicLong(name, initVal, create)
    def reference[T](name: String, @NullableField initVal: T, create: Boolean) : IgniteAtomicReference[T] = {
      ignite().atomicReference(name, initVal, create)
    }
    def stamped[T, S](name: String, @NullableField initVal: T, @NullableField initStamp: S, create: Boolean) : IgniteAtomicStamped[T, S] = {
      ignite().atomicStamped(name, initVal, initStamp, create)
    }
  }

  def countDownLatch(name: String, cnt: Int, autoDel: Boolean, create: Boolean) : IgniteCountDownLatch = {
    ignite().countDownLatch(name, cnt, autoDel, create)
  }
  def queue[T](name: String, cap: Int, @NullableField cfg : CollectionConfiguration) : IgniteQueue[T] = ignite().queue(name, cap, cfg)
  def set[T](name: String, @NullableField cfg : CollectionConfiguration) : IgniteSet[T] = ignite().set(name, cfg)
  def plugin[T <: IgnitePlugin](name: String) : T = ignite().plugin(name)
  def affinity[K](cacheName: String) : Affinity[K] = ignite().affinity(cacheName)


  protected def init() : Unit = {
    start(system.name)
    system.registerOnTermination(stop(system.name))
  }

  private def ignite() : Ignite = Ignition.ignite(system.name)

  // TODO. Implement idiomatic TypeSafe akka config (and do not depend on Spring Beans)
  private def start(name: String) : Unit = {
    IgnitionEx.start(getClass.getResourceAsStream("/reference_ignite.xml"), name, null)
  }

  private def stop(name: String): Unit = {
    Ignition.stop(name, true)
  }
}

