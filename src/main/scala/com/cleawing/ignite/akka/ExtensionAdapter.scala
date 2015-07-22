package com.cleawing.ignite.akka

import com.cleawing.ignite.akka.transport.{SourceProxyActor, RabbitSourceProxyActor}
import org.apache.ignite._
import org.apache.ignite.configuration._
import org.apache.ignite.internal.IgnitionEx
import org.apache.ignite.lifecycle.{LifecycleBean, LifecycleEventType}

import akka.actor.{Props, ActorRef, ExtendedActorSystem}

private[ignite] trait ExtensionAdapter {
  import com.cleawing.ignite.akka.LocalNodeWatcher.Restart

  protected def actorSystem: ExtendedActorSystem

  def actorOfRabbit(props: Props): ActorRef = {
    actorSystem.actorOf(RabbitSourceProxyActor(props))
  }
  def actorOfRabbit(props: Props, name: String) : ActorRef = {
    actorSystem.actorOf(RabbitSourceProxyActor(props), name)
  }
  def actorOfIgnite(props: Props): ActorRef = {
    actorSystem.actorOf(SourceProxyActor(props))
  }
  def actorOfIgnite(props: Props, name: String) : ActorRef = {
    actorSystem.actorOf(SourceProxyActor(props), name)
  }

  def restart() : Unit = {
    // Due lifeCycle listener will be restart automatically by ActorSystem
    Ignition.stop(actorSystem.name, true)
  }

  def shutdown() : Unit = {
    actorSystem.shutdown()
  }

  private[ignite] def init() : Unit = {
    start(actorSystem.systemActorOf(LocalNodeWatcher(), "local-node-watcher"))
    actorSystem.registerOnTermination {
      if (Ignition.state(actorSystem.name) != IgniteState.STOPPED) Ignition.stop(actorSystem.name, true)
      IgniteExtension.systems.remove(actorSystem.name)
    }
  }

  // TODO. Implement idiomatic TypeSafe akka config (and do not depend on ignite-spring)
  private[ignite] def start(monitor: ActorRef) : Unit = {
    val config = IgnitionEx.loadConfigurations(getClass.getResourceAsStream("/reference_ignite.xml"))
      .get1().toArray.apply(0).asInstanceOf[IgniteConfiguration]
    config.
      setGridName(actorSystem.name).
      setLifecycleBeans(lifeCycleBean(monitor))
    IgnitionEx.start(config)
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

