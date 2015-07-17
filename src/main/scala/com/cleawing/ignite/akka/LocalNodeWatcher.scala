package com.cleawing.ignite.akka

import akka.actor.{Props, Actor}
import com.cleawing.ignite.akka.LocalNodeWatcher.Restart
import org.apache.ignite.IgniteState
import scala.concurrent.duration._

class LocalNodeWatcher extends Actor {
  import context.dispatcher

  def receive = {
    case Restart => IgniteExtension(context.system).state() match {
      case IgniteState.STOPPED | IgniteState.STOPPED_ON_SEGMENTATION =>
        IgniteExtension(context.system).start(self)
      case IgniteState.STARTED => context.system.scheduler.scheduleOnce(100.milliseconds, self, Restart)
    }
  }
}

object LocalNodeWatcher {
  def apply() : Props = Props[LocalNodeWatcher]
  case object Restart
}
