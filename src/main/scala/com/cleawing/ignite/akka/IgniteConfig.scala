package com.cleawing.ignite.akka

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.CacheMemoryMode
import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cluster.ClusterNode
import org.apache.ignite.configuration.CollectionConfiguration
import org.apache.ignite.lang.IgnitePredicate

import scala.concurrent.duration.{Duration, FiniteDuration}

object IgniteConfig {
  final implicit class ConfigOps(val config: Config) extends AnyVal {
    def getCacheMemoryMode(path: String) : CacheMemoryMode = config.getString(path) match {
      case "ONHEAP_TIERED" => CacheMemoryMode.ONHEAP_TIERED
      case "OFFHEAP_TIERED" => CacheMemoryMode.OFFHEAP_TIERED
      case "OFFHEAP_VALUES" => CacheMemoryMode.OFFHEAP_VALUES
      case missed => throw new IllegalArgumentException(s"Only ONHEAP_TIERED, OFFHEAP_TIERED and OFFHEAP_VALUES accepted as cache-memory-mode.")
    }

    // From akka.utils.Helpers.ConfigOps
    def getMillisDuration(path: String): FiniteDuration = getDuration(path, TimeUnit.MILLISECONDS)
    def getNanosDuration(path: String): FiniteDuration = getDuration(path, TimeUnit.NANOSECONDS)

    private def getDuration(path: String, unit: TimeUnit): FiniteDuration =
      Duration(config.getDuration(path, unit), unit)
  }

  def buildCollectionConfig(
    atomicityMode: CacheAtomicityMode = CacheAtomicityMode.ATOMIC,
    cacheMode: CacheMode = CacheMode.PARTITIONED,
    memoryMode: CacheMemoryMode = CacheMemoryMode.ONHEAP_TIERED,
    nodeFilter : IgnitePredicate[ClusterNode] = null,
    backups : Int = 0,
    offHeapMaxMem : Long = -1,
    collocated : Boolean = false)(implicit ignite: IgniteAdapter) : CollectionConfiguration = {

    val cfg = ignite.Collection.config()
    cfg.setAtomicityMode(atomicityMode)
    cfg.setCacheMode(cacheMode)
    cfg.setMemoryMode(memoryMode)
    cfg.setNodeFilter(nodeFilter)
    cfg.setBackups(backups)
    cfg.setOffHeapMaxMemory(offHeapMaxMem)
    cfg.setCollocated(collocated)
    cfg
  }
}
