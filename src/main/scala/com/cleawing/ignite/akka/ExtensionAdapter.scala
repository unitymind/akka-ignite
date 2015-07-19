package com.cleawing.ignite.akka

import org.apache.ignite._
import org.apache.ignite.configuration._
import org.apache.ignite.internal.IgnitionEx
import org.apache.ignite.lifecycle.{LifecycleBean, LifecycleEventType}

import akka.actor.{ActorRef, ExtendedActorSystem}

private[ignite] trait ExtensionAdapter {
  import com.cleawing.ignite.akka.LocalNodeWatcher.Restart

  protected def system: ExtendedActorSystem

  def restart() : Unit = {
    // Due lifeCycle listener will be restart automatically
    stop0()
  }

  def shutdown() : Unit = system.shutdown()

  protected[ignite] def init() : Unit = {
    start(system.actorOf(LocalNodeWatcher()))
    system.registerOnTermination {
      stop0()
      IgniteExtension.systems.remove(system.name)
    }
  }

  // TODO. Implement idiomatic TypeSafe akka config (and do not depend on Spring Beans)
  protected[ignite] def start(monitor: ActorRef) : Unit = {
    val config = IgnitionEx.loadConfigurations(getClass.getResourceAsStream("/reference_ignite.xml"))
      .get1().toArray.apply(0).asInstanceOf[IgniteConfiguration]
    config.
      setGridName(system.name).
      setLifecycleBeans(lifeCycleBean(monitor))
    IgnitionEx.start(config)
  }

  private def stop0(): Unit = {
    Ignition.stop(system.name, true)
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

