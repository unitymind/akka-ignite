package com.cleawing.ignite.akka

import org.apache.ignite.lifecycle.{LifecycleBean, LifecycleEventType}


class IgniteLifecycleBean extends LifecycleBean {
  override def onLifecycleEvent(evt: LifecycleEventType) : Unit = {
    evt match {
      case LifecycleEventType.BEFORE_NODE_STOP => ActorSystem.terminateAll()
      case _ => // ignore
    }
  }
}

