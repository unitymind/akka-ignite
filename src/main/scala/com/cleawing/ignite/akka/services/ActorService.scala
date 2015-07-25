package com.cleawing.ignite.akka.services

import java.util.UUID

import akka.actor.{Props, ActorRef, ActorSystem}
import org.apache.ignite.services.ServiceContext
import org.jsr166.ConcurrentHashMap8

import com.cleawing.ignite
import scala.collection.immutable

trait ActorService extends IgniteService {
  protected def system : ActorSystem
}

case class ActorServiceImpl(clazz: Class[_], args: immutable.Seq[Any])
  extends IgniteServiceImpl with ActorService {

  @transient protected var system : ActorSystem = _
  @transient protected var service : ActorRef = _
  @transient protected var responders : ConcurrentHashMap8[(UUID, String), ActorRef] = _

  override def init(ctx: ServiceContext) : Unit = {
    super.init(ctx)
    system = ignite.system
    service = system.actorOf(Props(clazz, args :_*))
    responders = new ConcurrentHashMap8[(UUID, String), ActorRef]
  }

  override def cancel(ctx: ServiceContext) : Unit = {
    system.stop(service)
  }
}

object ActorService {
  def apply(clazz: Class[_], args: Any*) : ActorServiceImpl = new ActorServiceImpl(clazz, args.toList)
}
