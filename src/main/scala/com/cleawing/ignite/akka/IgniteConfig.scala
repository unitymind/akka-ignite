package com.cleawing.ignite.akka

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import org.apache.ignite.cache.CacheMemoryMode

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
}
