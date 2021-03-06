package com.cleawing.ignite.akka

import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.CacheMemoryMode
import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cluster.ClusterNode
import org.apache.ignite.configuration.CollectionConfiguration
import org.apache.ignite.lang.IgnitePredicate

object IgniteConfig {
  class CollectionBuilder() {
    private val cfg = new CollectionConfiguration()

    def setAtomicityMode(atomicityMode: CacheAtomicityMode) : this.type  = {
      cfg.setAtomicityMode(atomicityMode)
      this
    }

    def setCacheMode(cacheMode: CacheMode) : this.type = {
      cfg.setCacheMode(cacheMode)
      this
    }

    def setMemoryMode(memoryMode: CacheMemoryMode) : this.type = {
      cfg.setMemoryMode(memoryMode)
      this
    }

    def setNodeFilter(nodeFilter: IgnitePredicate[ClusterNode]) : this.type  = {
      cfg.setNodeFilter(nodeFilter)
      this
    }

    def setBackups(backups: Int) : this.type = {
      cfg.setBackups(backups)
      this
    }

    def setOffHeapMaxMemory(offHeapMaxMem: Long) : this.type = {
      cfg.setOffHeapMaxMemory(offHeapMaxMem)
      this
    }

    def setCollocated(collocated: Boolean) : this.type = {
      cfg.setCollocated(collocated)
      this
    }

    def build() : CollectionConfiguration = cfg
  }

  object CollectionBuilder {
    def apply() = new CollectionBuilder()
  }
}
