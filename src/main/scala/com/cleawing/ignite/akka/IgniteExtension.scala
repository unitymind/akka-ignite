package com.cleawing.ignite.akka

import java.util.UUID

import akka.actor._
import com.cleawing.ignite.akka.remote.{Utils, RemoteManager}
import com.cleawing.ignite.{Implicits, IgniteAdapter}
import com.typesafe.config.Config
import org.apache.ignite.cache.CacheMode
import com.cleawing.ignite.akka.services.DeploymentActorService.GlobalDescriptor
import org.apache.ignite.{Ignite, IgniteCheckedException}
import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.internal.IgnitionEx
import org.apache.ignite.lifecycle.{LifecycleEventType, LifecycleBean}
import org.apache.ignite.resources.IgniteInstanceResource

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object IgniteExtension
  extends ExtensionId[IgniteExtensionImpl]
  with ExtensionIdProvider {

  override def lookup() = IgniteExtension

  override def createExtension(system: ExtendedActorSystem) = {
    val extension = IgniteExtensionImpl(system.settings.config.getConfig("akka.ignite"), system.name)
    system.systemActorOf(IgniteGuardian.props(), "ignite")
    system.registerOnTermination {
      extension.stop(cancel = true)
    }
    sys.addShutdownHook {
      extension.stop(cancel = true)
      Await.result(system.terminate(), Duration.Inf)
    }
    extension
  }

}
class IgniteExtensionImpl private (val configuration: IgniteConfiguration)
  extends Extension with IgniteAdapter {

}

private object IgniteExtensionImpl {
  import Implicits.ConfigOps
  import scala.collection.JavaConverters._

  def apply(config: Config, name: String) : IgniteExtensionImpl = {
    import config._

    def buildConfiguration() : IgniteConfiguration = {
      // TODO. Be careful with class loader
      (if (this.getClass.getResource(getString("config-resource-path")) != null) {
        IgnitionEx.loadConfigurations(this.getClass.getResourceAsStream("/ignite.xml"))
          .get1().toArray.apply(0).asInstanceOf[IgniteConfiguration]
      } else new IgniteConfiguration())

        .setGridName(name)
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

    val cfg =
      try {
        if (hasPath("external-config-path")) {
          val cfg = IgnitionEx.loadConfiguration(getString("external-config-path")).get1()
          if (cfg.getGridName == null) cfg.setGridName(getString("name"))
          cfg
        } else {
          buildConfiguration()
        }
      } catch {
        case t: IgniteCheckedException if getBoolean("continue-on-external-config-error") =>
          buildConfiguration()
      }

    cfg.setLifecycleBeans(new LifecycleBean {
      @IgniteInstanceResource
      var ignite : Ignite = _

      override def onLifecycleEvent(evt: LifecycleEventType): Unit = {
        evt match {
          case LifecycleEventType.BEFORE_NODE_STOP =>
            if (!(ignite.cluster().localNode().isClient && ignite.cluster().nodes().size() == 1))
              ignite.cache[UUID, Utils.HostPort](RemoteManager.remoteCacheCfg.getName)
                .remove(ignite.cluster().localNode().id)
          case _ =>
        }
      }
    })

    new IgniteExtensionImpl(cfg)
  }
}
