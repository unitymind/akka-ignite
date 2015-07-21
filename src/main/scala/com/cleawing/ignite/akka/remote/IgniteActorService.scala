package com.cleawing.ignite.akka.remote

import akka.actor.{PoisonPill, Props, ActorRef, ExtendedActorSystem}
import com.cleawing.ignite.IgniteAdapter
import com.cleawing.ignite.akka.{IgniteExtensionImpl, IgniteExtension}
import org.apache.ignite.Ignite
import org.apache.ignite.resources.IgniteInstanceResource
import org.apache.ignite.services.{ServiceContext, Service}
import scala.collection.mutable.HashMap
import scala.language.existentials


trait IgniteActorService {
  protected def system : ExtendedActorSystem
  protected def ignite : IgniteAdapter
  protected def target : ActorRef
  protected def responders : HashMap[(String, String), ActorRef]
  protected def executionId : String
  protected def name : String

  def !(pe: ProxyEnvelope) : Unit = {
    val responder = if (responders.contains((pe.sender, pe.deploymentName))) {
      responders((pe.sender, pe.deploymentName))
    } else {
      val ref = system.actorOf(IgniteProxyResponderActor(pe.sender, pe.deploymentName), s"responder-${executionId}")
      responders.put((pe.sender, pe.deploymentName), ref)
      ref
    }
    target.tell(pe.message, responder)
  }
}

object IgniteActorService {
  def apply(clazz: Class[_], args: Seq[Any]) : IgniteActorServiceImpl = {
    new IgniteActorServiceImpl(clazz, args)
  }
}

private[ignite] case class IgniteActorServiceImpl(clazz: Class[_], args: Seq[Any])
  extends Service with IgniteActorService {

  @IgniteInstanceResource
  protected var _ignite: Ignite = _
  protected var _executionId : String = _
  protected var _name : String = _
  protected var _target : ActorRef = _
  protected val responders : HashMap[(String, String), ActorRef] = HashMap.empty[(String, String), ActorRef]

  def cancel(ctx: ServiceContext) : Unit = {
    _target ! PoisonPill
    responders.values.foreach(r => r ! PoisonPill)
  }

  def init(ctx: ServiceContext) : Unit = {
    _executionId = ctx.executionId().toString
    _name = ctx.name()
    _target = system.actorOf(Props(clazz, args :_*), s"target-${_executionId}")
  }

  def execute(ctx: ServiceContext) : Unit = ()

  def executionId : String = _executionId
  def name : String = _name

  final protected def system : ExtendedActorSystem = IgniteExtension.resolveActorSystem(_ignite.name()).get
  final protected def ignite : IgniteExtensionImpl  = IgniteExtension(system)
  final protected def target : ActorRef = _target
}
