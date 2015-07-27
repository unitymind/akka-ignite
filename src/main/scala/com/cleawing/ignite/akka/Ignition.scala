package com.cleawing.ignite.akka


import akka.actor._
import com.cleawing.ignite
import com.cleawing.ignite.akka.services.{DeploymentActorService, ServiceProxyRouter}

trait Ignition extends { this: Actor =>
  import Ignition._
  val ignition = ignite.ignite()
  private def remote() = ignite.grid().Services(ignite.grid().cluster().forRemotes())

  final implicit class ActorContextOps(val context: ActorContext) {
    def serviceOf(props: Props, name: String, totalCnt: Int = 1, maxPerNodeCnt: Int = 1) : ActorRef = {
      val ref = context.actorOf(ServiceProxyRouter())
      remote().deployMultiple(
        buildRemotePathString(ref.path),
        DeploymentActorService(props, Some(buildRemotePathString(self.path))),
        totalCnt,
        maxPerNodeCnt
      )
      ref
    }
  }
}

object Ignition {
  def buildRemotePathString(path: ActorPath) : String = {
    path.toSerializationFormat
      .replace(
        ignite.system().toString + "/",
        ignite.rootPath.toSerializationFormat)
  }
}
