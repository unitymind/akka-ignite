package com.cleawing.ignite.akka

import akka.actor.ActorSystem
import org.apache.ignite.services.{ServiceContext, Service}

import scala.collection.mutable.HashMap

trait ActorSystemService {
  def apply(name: String) : ActorSystem
}

object ActorSystemService {
  def apply() : ActorSystemServiceImpl = new ActorSystemServiceImpl()
}

class ActorSystemServiceImpl
  extends Service with ActorSystemService {

  protected val systems = HashMap.empty[String, ActorSystem]

  override def init(ctx: ServiceContext) : Unit = {

  }

  override def execute(ctx: ServiceContext) : Unit = {

  }

  override def cancel(ctx: ServiceContext) : Unit = {
    systems.values.foreach(_.terminate())
  }

  def apply(name: String) : ActorSystem = obtainActorSystem(name)

  private def obtainActorSystem(name: String) : ActorSystem = {
    systems.get(name) match {
      case Some(system) => system
      case None => Some(systems.getOrElseUpdate(name, ActorSystem(name)))
        .map { system =>
        system.registerOnTermination { systems.remove(name) }
        system
      }.get
    }
  }

}
