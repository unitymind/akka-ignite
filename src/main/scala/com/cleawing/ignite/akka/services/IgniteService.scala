package com.cleawing.ignite.akka.services

import java.util.UUID

import akka.actor.{Actor, ActorSystem, Props, ActorRef}
import org.apache.ignite.Ignite
import org.apache.ignite.resources.IgniteInstanceResource
import org.apache.ignite.services.{ServiceContext, Service}
import scaldi.Injector
import scaldi.akka.AkkaInjectable

import scala.collection.immutable

trait IgniteService {
  def name : String
  def executionId : UUID
}

abstract class IgniteServiceImpl extends Service with IgniteService {
  @IgniteInstanceResource
  protected var _ignite: Ignite = _
  protected var _executionId : UUID = _
  protected var _name : String = _

  def init(ctx: ServiceContext) : Unit = {
    _executionId = ctx.executionId()
    _name = ctx.name()
  }

  def executionId : UUID = _executionId
  def name : String = _name
}

trait ActorService extends IgniteService {
  protected def system : ActorSystem
}

private[ignite] class ActorServiceImpl(clazz: Class[_], args: immutable.Seq[Any])
  extends IgniteServiceImpl
  with ActorService
  with AkkaInjectable {

  import com.cleawing.ignite.injector

  protected val system = inject [ActorSystem]
  protected var _target : ActorRef = _

  override def init(ctx: ServiceContext) : Unit = {
    super.init(ctx)
    _target = system.actorOf(Props(clazz, args :_*))
  }

  def execute(ctx: ServiceContext) : Unit = ()

  def cancel(ctx: ServiceContext) : Unit = {
    if (!ctx.isCancelled) {
      system.stop(_target) // TODO. Or PoisonPill?
    }
  }
}

object ActorService {
  def apply(clazz: Class[_], args: Any*) : ActorServiceImpl = new ActorServiceImpl(clazz, args.toList)
}

