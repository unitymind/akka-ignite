package com.cleawing.ignite.playground

import akka.actor.PoisonPill
import com.cleawing.ignite.akka.IgniteService
import org.apache.ignite.services.ServiceContext

class EchoService extends IgniteService {

  override def init(ctx: ServiceContext) : Unit = {
    _guardian = system.actorOf(EchoActor().withMailbox("akka.ignite.mailbox.unbounded"), s"${ctx.name()}-${ctx.executionId().toString}")
  }

  override def execute(ctx: ServiceContext) : Unit = {
    _guardian ! s"Started [${this.getClass.toString}]: ${ctx.executionId().toString}"
  }

  override def cancel(ctx: ServiceContext) : Unit = {
    _guardian ! s"Canceled [${this.getClass.toString}]: ${ctx.executionId().toString}"
    _guardian ! PoisonPill
  }
}
