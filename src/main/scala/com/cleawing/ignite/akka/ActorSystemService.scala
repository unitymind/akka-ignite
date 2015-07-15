package com.cleawing.ignite.akka

import akka.actor.{ActorSystem => AkkaActorSystem}
import com.typesafe.config.Config
import org.apache.ignite.IgniteException
import org.apache.ignite.services.{ServiceContext, Service}

import scala.collection.mutable.HashMap
import java.util.concurrent.{ CountDownLatch => CDL }

import scala.concurrent.ExecutionContext

import com.cleawing.ignite._

trait ActorSystemService extends IgniteService

object ActorSystemService {
  val name = "actorServices"

  def deploy() : Unit = {
    services.deployNodeSingleton(name, new ActorSystemServiceImpl)
  }

  def stop(): Unit = {
    services.cancel(name)
  }
}

class ActorSystemServiceImpl
  extends Service with ActorSystemService {

  protected var name: String = ""
  protected var isDeployed: Boolean = false
  protected var inCancel: Boolean = false
  protected val systems = HashMap.empty[String, AkkaActorSystem]

  // Check capability of subscription to Ignite events and maintain deployed state
  override def init(ctx: ServiceContext) : Unit = {
    isDeployed = true
    name = ctx.name
  }

  override def execute(ctx: ServiceContext) : Unit = {

  }

  override def cancel(ctx: ServiceContext) : Unit = {
    inCancel = true
    val latch = new CDL(systems.size)
    systems.values.foreach { system =>
      system.terminate()
      latch.countDown()
    }
    latch.await()
    isDeployed = false
  }
  
  def apply(name: String, config: Option[Config] = None, classLoader: Option[ClassLoader] = None, defaultExecutionContext: Option[ExecutionContext] = None): AkkaActorSystem = {
    (isDeployed, inCancel) match {
      case (false, _) => throw new IgniteException(s"Service '${this.name}' has not deployed!")
      case (_, true) => throw new IgniteException(s"Service '${this.name}' is shutting down!")
      case _ =>
    }

    systems.getOrElse(name, {
      val system = systems.getOrElseUpdate(name, AkkaActorSystem(name, config, classLoader, defaultExecutionContext))
      system.registerOnTermination {
        systems.remove(name)
      }
      system
    })
  }
}
