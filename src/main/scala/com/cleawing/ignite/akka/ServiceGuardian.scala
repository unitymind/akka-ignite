package com.cleawing.ignite.akka

import akka.actor.{PoisonPill, Props, Actor}

class ServiceGuardian extends Actor with Ignition {
  import ServiceGuardian._

  def receive = {
    case Start(props, executionId) =>
      context.actorOf(props, executionId)
    case Tell(executionId, msg) =>
      context.actorSelection(executionId) ! msg
    case Stop(executionId) =>
      context.actorSelection(executionId) ! PoisonPill
  }
}

object ServiceGuardian {
  def apply() : Props = Props[ServiceGuardian]
  case class Start(props: Props, executionId: String)
  case class Stop(executionId: String)
  case class Tell(executionId: String, msg: Any)
}
