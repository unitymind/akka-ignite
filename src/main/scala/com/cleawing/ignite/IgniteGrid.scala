package com.cleawing.ignite

import com.typesafe.config.Config
import org.apache.ignite.IgniteCheckedException
import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.internal.IgnitionEx
import scala.collection.JavaConverters._

class IgniteGrid private[ignite] (val configuration: IgniteConfiguration) extends IgniteAdapter {
  def shutdown(cancel: Boolean = false) : Unit = stop(cancel)
}

private[ignite] object IgniteGridFactory {
  import Implicits.ConfigOps

  def apply(config: Config) : IgniteGrid = {
    def buildConfiguration() : IgniteConfiguration = {
      (if (getClass.getResource(config.getString("config-resource-path")) != null) {
        IgnitionEx.loadConfigurations(getClass.getResourceAsStream("/ignite.xml"))
          .get1().toArray.apply(0).asInstanceOf[IgniteConfiguration]
      } else new IgniteConfiguration())

        .setGridName(config.getString("name"))
        .setClientMode(config.getBoolean("client-mode"))

        .setPeerClassLoadingEnabled(config.getBoolean("peer-class-loading.enabled"))
        .setPeerClassLoadingLocalClassPathExclude(config.getStringList("peer-class-loading.loading-local-exclude").asScala:_*)
        .setPeerClassLoadingMissedResourcesCacheSize(config.getInt("peer-class-loading.missed-resources-cache-size"))
        .setDeploymentMode(config.getDeploymentMode("peer-class-loading.deployment-mode"))

        .setMetricsHistorySize(config.getInt("metrics.history-size"))
        .setMetricsExpireTime(config.getLong("metrics.expire-time"))
        .setMetricsUpdateFrequency(config.getLong("metrics.update-frequency"))
        .setMetricsLogFrequency(config.getLong("metrics.log-frequency"))

        .setNetworkTimeout(config.getLong("network.timeout"))
        .setNetworkSendRetryDelay(config.getLong("network.send-retry-delay"))
        .setNetworkSendRetryCount(config.getInt("network.send-retry-count"))
    }

    try {
      if (config.hasPath("external-config-path")) {
        val cfg = IgnitionEx.loadConfiguration(config.getString("external-config-path")).get1()
        if (cfg.getGridName == null) cfg.setGridName(config.getString("name"))
        new IgniteGrid(cfg)
      } else {
        new IgniteGrid(buildConfiguration())
      }
    } catch {
      case t: IgniteCheckedException if config.getBoolean("continue-on-external-config-error") =>
        new IgniteGrid(buildConfiguration())
    }
  }
}
