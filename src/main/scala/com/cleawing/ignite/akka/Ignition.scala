package com.cleawing.ignite.akka

import akka.actor._
import com.cleawing.ignite.akka.services.{DeploymentActorService, ServiceProxyRouter}

trait Ignition extends { this: Actor =>
  protected val grid = IgniteExtension(context.system)

  def buildRemotePathString(path: ActorPath) : String = {
    path.toSerializationFormat
      .replace(
        context.system.toString,
        s"${grid.cluster().localNode().id}@")
  }

  final implicit class ActorSystemOps(val system: ActorSystem) {
    val rootPath : ActorPath = ActorPath.fromString(system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress.toString)
  }

  final implicit class ActorContextOps(val context: ActorContext) {
    private def remote() = grid.Services(grid.cluster().forRemotes()).withAsync()

    def serviceOf(props: Props, name: String, totalCnt: Int = 1, maxPerNodeCnt: Int = 1) : ActorRef = {
      val ref = context.actorOf(ServiceProxyRouter(), name)
      remote().deployMultiple(
        buildRemotePathString(ref.path),
        DeploymentActorService(props),
        totalCnt,
        maxPerNodeCnt
      )
      ref
    }
  }
}