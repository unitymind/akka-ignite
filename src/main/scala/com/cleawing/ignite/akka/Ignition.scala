package com.cleawing.ignite.akka


import _root_.akka.actor.{ActorSelection, Props, ActorContext, Actor}
import com.cleawing.ignite
import com.cleawing.ignite.akka.services.ActorService

trait Ignition extends { this: Actor =>
  val ignition = ignite.ignite()
  private def remote() = ignite.grid().Services(ignite.grid().cluster().forRemotes())
  private def proxy(name: String) = remote().serviceProxy[ActorService](name, classOf[ActorService], false)

  final implicit class ActorContextOps(val system: ActorContext) {
    def serviceOf(props: Props, name: String, totalCnt: Int = 1, maxPerNodeCnt: Int = 1) : ActorSelection = {
      val selfPath = self.path.toSerializationFormat
        .replace(
          ignite.system().toString + "/",
          ignite.rootPath().toSerializationFormat)
      remote().deployMultiple(selfPath + s"/$name", ActorService(props, Some(selfPath)), totalCnt, maxPerNodeCnt)
      context.system.actorSelection(proxy(selfPath + s"/$name").path())
    }
  }
}

object Ignition {

}
