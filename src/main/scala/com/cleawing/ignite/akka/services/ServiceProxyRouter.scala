package com.cleawing.ignite.akka.services

import akka.actor.{Terminated, ActorRef, Props, Actor}
import akka.routing.{RoundRobinRoutingLogic, Router}

import com.cleawing.ignite
import com.cleawing.ignite.akka.Ignition

class ServiceProxyRouter extends Actor {
  import ServiceProxyRouter._

  private var router = Router(RoundRobinRoutingLogic())

  def receive = {
    case Register(ref) =>
      router = router.addRoutee(ref)
      context.watch(ref)
      println(s"Routees count after Register: ${router.routees.size}")
    case Terminated(ref) =>
      router = router.removeRoutee(ref)
      println(s"Routees count after Terminated: ${router.routees.size}")

    case msg => router.route(msg, sender())
  }

  override def postStop(): Unit = {
    ignite.grid().Services().cancel(Ignition.buildRemotePathString(self.path))
  }
}

object ServiceProxyRouter {
  def apply() : Props = Props[ServiceProxyRouter]

  case class Register(routee: ActorRef)
  case class AddInstance(path: String)
  case class RemoveInstance(path: String)
}



