package com.cleawing.ignite.akka.services

import akka.actor.{ExtendedActorSystem, ActorSystem}
import com.cleawing.ignite.{IgniteGrid, Injector}
import org.apache.ignite.services.ServiceContext

trait ActorServiceCollector extends IgniteService {
  protected def system : ActorSystem
  protected[ignite] def reply(en: ProxyEnvelope) : Unit
}

private[ignite] class ActorServiceCollectorImpl(servicePath: String)
  extends IgniteServiceImpl with ActorServiceCollector {

  @transient protected var system : ActorSystem = _
  @transient protected var grid : IgniteGrid = _

  override def init(ctx: ServiceContext) : Unit = {
    super.init(ctx)
    system = Injector.actorSystem()
    grid = Injector.grid()
  }

  def reply(pe: ProxyEnvelope) : Unit = {
    if (pe.nodeId == grid.cluster().localNode().id()) {
      val provider = system.asInstanceOf[ExtendedActorSystem].provider
      provider.resolveActorRef(pe.sender).tell(pe.message, provider.resolveActorRef(servicePath))
    }
  }
}

object ActorServiceCollector {
  def apply(servicePath: String) = new ActorServiceCollectorImpl(servicePath)
}
