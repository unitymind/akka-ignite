package com.cleawing.ignite.akka.services

import java.util.UUID

import akka.actor.{Props, ActorRef, ActorSystem}
import com.cleawing.ignite.Injector
import org.apache.ignite.services.ServiceContext
import org.jsr166.ConcurrentHashMap8

import scala.collection.immutable

trait ActorService extends IgniteService {
  protected def system : ActorSystem
  protected[ignite] def tell(en: ProxyEnvelope) : Unit
}

private[ignite] case class ActorServiceImpl(clazz: Class[_], args: immutable.Seq[Any])
  extends IgniteServiceImpl with ActorService {

  @transient protected var system : ActorSystem = _
  @transient protected var service : ActorRef = _
  @transient protected var responders : ConcurrentHashMap8[(UUID, String), ActorRef] = _

  override def init(ctx: ServiceContext) : Unit = {
    super.init(ctx)
    system = Injector.actorSystem()
    service = system.actorOf(Props(clazz, args :_*))
    responders = new ConcurrentHashMap8[(UUID, String), ActorRef]
  }

  protected[ignite] def tell(pe: ProxyEnvelope) : Unit = {
    val responder = if (responders.containsKey((pe.nodeId, pe.sender))) {
      responders.get((pe.nodeId, pe.sender))
    } else {
      val ref = system.actorOf(InboundServiceActor(name, pe.sender, pe.nodeId))
      responders.put((pe.nodeId, pe.sender), ref)
      ref
    }
    service.tell(pe.message, responder)
  }

  override def cancel(ctx: ServiceContext) : Unit = {
    system.stop(service)
  }
}

object ActorService {
  def apply(clazz: Class[_], args: Any*) : ActorServiceImpl = new ActorServiceImpl(clazz, args.toList)
}
