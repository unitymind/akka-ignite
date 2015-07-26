package com.cleawing.ignite

import com.typesafe.config.Config
import org.apache.ignite.IgniteCheckedException
import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.internal.IgnitionEx
import scala.collection.JavaConverters._

class IgniteGrid private[ignite] (val configuration: IgniteConfiguration) extends IgniteAdapter {
  def shutdown(cancel: Boolean = true) : Unit = stop(cancel)
}

private[ignite] object IgniteGridFactory {
  import Implicits.ConfigOps

  def apply(config: Config) : IgniteGrid = {
    import config._

    def buildConfiguration() : IgniteConfiguration = {
      // TODO. Be careful with class loader
      (if (this.getClass.getResource(getString("config-resource-path")) != null) {
        IgnitionEx.loadConfigurations(this.getClass.getResourceAsStream("/ignite.xml"))
          .get1().toArray.apply(0).asInstanceOf[IgniteConfiguration]
      } else new IgniteConfiguration())

        .setGridName(getString("name"))
        .setClientMode(getBoolean("client-mode"))

        .setPeerClassLoadingEnabled(getBoolean("peer-class-loading.enabled"))
        .setPeerClassLoadingLocalClassPathExclude(getStringList("peer-class-loading.loading-local-exclude").asScala:_*)
        .setPeerClassLoadingMissedResourcesCacheSize(getInt("peer-class-loading.missed-resources-cache-size"))
        .setDeploymentMode(config.getDeploymentMode("peer-class-loading.deployment-mode"))

        .setMetricsHistorySize(getInt("metrics.history-size"))
        .setMetricsExpireTime(getLong("metrics.expire-time"))
        .setMetricsUpdateFrequency(getLong("metrics.update-frequency"))
        .setMetricsLogFrequency(getLong("metrics.log-frequency"))

        .setNetworkTimeout(getLong("network.timeout"))
        .setNetworkSendRetryDelay(getLong("network.send-retry-delay"))
        .setNetworkSendRetryCount(getInt("network.send-retry-count"))
        .setLocalHost(if (getIsNull("network.localhost")) null else getString("network.localhost"))
    }

    try {
      if (hasPath("external-config-path")) {
        val cfg = IgnitionEx.loadConfiguration(getString("external-config-path")).get1()
        if (cfg.getGridName == null) cfg.setGridName(getString("name"))
        new IgniteGrid(cfg)
      } else {
        new IgniteGrid(buildConfiguration())
      }
    } catch {
      case t: IgniteCheckedException if getBoolean("continue-on-external-config-error") =>
        new IgniteGrid(buildConfiguration())
    }
  }
}
