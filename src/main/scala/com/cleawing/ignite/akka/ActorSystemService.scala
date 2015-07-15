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

private[ignite] object ActorSystemService {
  val name = "actorServices"

  def deploy() : Unit   = services.deployNodeSingleton(name, new ActorSystemServiceImpl)
  def undeploy(): Unit  = services.cancel(name)
}

private[ignite] class ActorSystemServiceImpl
  extends Service with ActorSystemService {

  protected var isDeployed: Boolean = false
  protected var inTerminate: Boolean = false
  protected val systems = HashMap.empty[String, AkkaActorSystem]

  override def init(ctx: ServiceContext) : Unit = {
    isDeployed = true
  }

  override def execute(ctx: ServiceContext) : Unit = {

  }

  override def cancel(ctx: ServiceContext) : Unit = {
    if (!ctx.isCancelled) {
      inTerminate = true
      terminateAll()
      isDeployed = false
    }
  }

  def terminateAll() : Unit = {
    val latch = new CDL(systems.size)
    systems.values.foreach { system =>
      system.terminate()
      latch.countDown()
    }
    latch.await()
  }

  def apply(name: String, config: Option[Config] = None, classLoader: Option[ClassLoader] = None, defaultExecutionContext: Option[ExecutionContext] = None): AkkaActorSystem = {
    (isDeployed, inTerminate) match {
      case (false, _) => throw new IgniteException(s"Service '${ActorSystemService.name}' has not deployed!")
      case (_, true) => throw new IgniteException(s"Service '${ActorSystemService.name}' is terminating!")
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
