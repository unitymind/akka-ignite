package com.cleawing.ignite.akka

import org.apache.ignite._
import org.apache.ignite.configuration._
import org.apache.ignite.internal.IgnitionEx
import org.apache.ignite.lifecycle.{LifecycleBean, LifecycleEventType}

import akka.actor.{ActorRef, ExtendedActorSystem}

private[ignite] trait ExtensionAdapter {
  import com.cleawing.ignite.akka.LocalNodeWatcher.Restart

  protected def actorSystem: ExtendedActorSystem

  def restart() : Unit = {
    // Due lifeCycle listener will be restart automatically
    stop0()
  }

  def shutdown() : Unit = actorSystem.shutdown()

  protected[ignite] def init() : Unit = {
    start(actorSystem.actorOf(LocalNodeWatcher(), "local-node-watcher"))
    actorSystem.registerOnTermination {
      stop0()
      IgniteExtension.systems.remove(actorSystem.name)
    }
  }

  // TODO. Implement idiomatic TypeSafe akka config (and do not depend on ignite-spring)
  protected[ignite] def start(monitor: ActorRef) : Unit = {
    val config = IgnitionEx.loadConfigurations(getClass.getResourceAsStream("/reference_ignite.xml"))
      .get1().toArray.apply(0).asInstanceOf[IgniteConfiguration]
    config.
      setGridName(actorSystem.name).
      setLifecycleBeans(lifeCycleBean(monitor))
    IgnitionEx.start(config)
  }

  private def stop0(): Unit = {
    Ignition.stop(actorSystem.name, true)
  }

  private def lifeCycleBean(monitor: ActorRef) = new LifecycleBean {
    override def onLifecycleEvent(evt: LifecycleEventType): Unit = {
      evt match {
        case LifecycleEventType.AFTER_NODE_STOP => monitor ! Restart
        case _ => // ignore others
      }
    }
  }
}

