package com.cleawing.ignite.akka.dispatch

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch._
import com.cleawing.ignite.akka.{IgniteConfig, IgniteExtensionImpl, IgniteExtension}
import com.typesafe.config.Config
import org.apache.ignite.configuration.CollectionConfiguration
import org.apache.ignite.cache.{CacheMode, CacheMemoryMode}
import com.cleawing.ignite.akka.IgniteConfig.ConfigOps

case class IgniteUnboundedMailbox(_memoryMode: CacheMemoryMode)
  extends MailboxType with ProducesMessageQueue[IgniteUnboundedQueueBasedMessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = {
    this(config.getCacheMemoryMode("cache-memory-mode"))
  }

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = {
    (owner, system) match {
      case (Some(o), Some(s)) =>
        implicit val ignite = IgniteExtension(s)
        val cfg = IgniteConfig.buildCollectionConfig(cacheMode = CacheMode.LOCAL, memoryMode = _memoryMode)
        new IgniteUnboundedQueueBasedMessageQueue(o.path.toStringWithoutAddress, cfg, ignite)
    }
  }
}

class IgniteUnboundedQueueBasedMessageQueue(queueName: String, cfg: CollectionConfiguration, ignite: IgniteExtensionImpl)
  extends UnboundedQueueBasedMessageQueue {

  import akka.serialization.JavaSerializer.currentSystem

  final val queue = ignite.Collection.queue[Envelope](queueName, 0, cfg)

  override def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
    currentSystem.withValue(ignite.actorSystem) { super.enqueue(receiver, handle) }
  }

  override def dequeue(): Envelope = {
    currentSystem.withValue(ignite.actorSystem) { super.dequeue() }
  }

  override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
    super.cleanUp(owner, deadLetters)
    queue.close()
  }
}
