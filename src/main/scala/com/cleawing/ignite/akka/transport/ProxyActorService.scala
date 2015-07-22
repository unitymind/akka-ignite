package com.cleawing.ignite.akka.transport

import akka.actor.{PoisonPill, Props, ActorRef, ExtendedActorSystem}
import com.cleawing.ignite.IgniteAdapter
import com.cleawing.ignite.akka.{IgniteExtensionImpl, IgniteExtension}
import org.apache.ignite.Ignite
import org.apache.ignite.resources.IgniteInstanceResource
import org.apache.ignite.services.{ServiceContext, Service}
import scala.collection.mutable.HashMap
import scala.language.existentials


trait ProxyActorService {
  protected def executionId : String
  protected def name : String
}

object ProxyActorService {
  def apply(clazz: Class[_], args: Seq[Any], deploymentId :String) : ProxyActorServiceImpl = {
    new ProxyActorServiceImpl(clazz, args, deploymentId)
  }
}

case class ProxyActorServiceImpl(clazz: Class[_], args: Seq[Any], deploymentId :String)
  extends Service with ProxyActorService {

  @IgniteInstanceResource
  protected var _ignite: Ignite = _
  protected var _executionId : String = _
  protected var _name : String = _
  protected var _targetContainer : ActorRef = _

  def cancel(ctx: ServiceContext) : Unit = {
    _targetContainer ! PoisonPill
  }

  def init(ctx: ServiceContext) : Unit = {
    _executionId = ctx.executionId().toString
    _name = ctx.name()
    _targetContainer = system.actorOf(TargetProxyActor(Props(clazz, args :_*), deploymentId), s"proxy-${_executionId}")
  }

  def execute(ctx: ServiceContext) : Unit = ()

  def executionId : String = _executionId
  def name : String = _name

  private lazy val system : ExtendedActorSystem = IgniteExtension.resolveActorSystem(_ignite.name()).get
}
