package com.cleawing.ignite.akka

import akka.actor.{Actor, Props}
import com.cleawing.ignite.akka.LocalNodeWatcher.Restart
import org.apache.ignite.IgniteState

import scala.concurrent.duration._

private[ignite] class LocalNodeWatcher extends Actor {
  import context.dispatcher

  def receive = {
    case Restart => IgniteExtension(context.system).state() match {
      case IgniteState.STOPPED | IgniteState.STOPPED_ON_SEGMENTATION =>
        IgniteExtension(context.system).start(self)
      case IgniteState.STARTED => context.system.scheduler.scheduleOnce(100.milliseconds, self, Restart)
    }
  }
}

private[ignite] object LocalNodeWatcher {
  def apply() : Props = Props[LocalNodeWatcher]
  case object Restart
}
