package com.cleawing.ignite.playground

import akka.actor.PoisonPill
import com.cleawing.ignite.akka.IgniteService
import org.apache.ignite.services.ServiceContext

class EmptyService extends IgniteService {

  override def init(ctx: ServiceContext) : Unit = {
    system.actorOf(EchoActor(), ctx.executionId().toString)
  }

  override def cancel(ctx: ServiceContext) : Unit = {
    system.actorSelection(s"/user/${ctx.executionId()}") ! PoisonPill
  }
}
