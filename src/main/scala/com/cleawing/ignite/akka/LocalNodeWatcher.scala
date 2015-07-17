package com.cleawing.ignite.akka

import akka.actor.{Props, Actor}
import com.cleawing.ignite.akka.LocalNodeWatcher.Restart
import org.apache.ignite.IgniteCheckedException


class LocalNodeWatcher extends Actor {
  def receive = {
    case Restart => restart()
  }

  def restart() : Unit = {
    try {
      IgniteExtension(context.system).start(self)
    } catch {
      case _: IgniteCheckedException => restart()
    }
  }
}

object LocalNodeWatcher {
  def apply() : Props = Props[LocalNodeWatcher]
  case object Restart
}
