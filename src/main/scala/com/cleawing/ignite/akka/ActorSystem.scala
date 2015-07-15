package com.cleawing.ignite.akka

import akka.actor.{ActorSystem => AkkaActorSystem}
import com.typesafe.config.Config
import com.cleawing.ignite._

import scala.concurrent.ExecutionContext

object ActorSystem {
  private def actorSystemService = services.service[ActorSystemServiceImpl](ActorSystemService.name)

  def create(): AkkaActorSystem = apply()
  def create(name: String): AkkaActorSystem = apply(name)
  def create(name: String, config: Config): AkkaActorSystem = apply(name, config)
  def create(name: String, config: Config, classLoader: ClassLoader): AkkaActorSystem = apply(name, config, classLoader)
  def create(name: String, config: Config, classLoader: ClassLoader, defaultExecutionContext: ExecutionContext): AkkaActorSystem = apply(name, Option(config), Option(classLoader), Option(defaultExecutionContext))
  def apply(): AkkaActorSystem = apply("default")
  def apply(name: String): AkkaActorSystem = apply(name, None, None, None)
  def apply(name: String, config: Config): AkkaActorSystem = apply(name, Option(config), None, None)
  def apply(name: String, config: Config, classLoader: ClassLoader): AkkaActorSystem = apply(name, Option(config), Option(classLoader), None)
  def apply(name: String, config: Option[Config] = None, classLoader: Option[ClassLoader] = None, defaultExecutionContext: Option[ExecutionContext] = None): AkkaActorSystem = {
    if (actorSystemService == null) {
      ActorSystemService.deploy()
    }
    actorSystemService(name, config, classLoader, defaultExecutionContext)
  }
}
