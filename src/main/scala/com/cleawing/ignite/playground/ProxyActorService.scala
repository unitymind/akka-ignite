package com.cleawing.ignite.playground

import akka.actor.{ActorRef, ExtendedActorSystem, PoisonPill, Props}
import com.cleawing.ignite.akka.IgniteExtension
import org.apache.ignite.Ignite
import org.apache.ignite.resources.IgniteInstanceResource
import org.apache.ignite.services.{Service, ServiceContext}

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
