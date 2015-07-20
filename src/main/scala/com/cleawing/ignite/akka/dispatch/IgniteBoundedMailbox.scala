package com.cleawing.ignite.akka.dispatch

import akka.actor.{ActorSystem, ActorRef}
import akka.dispatch._
import com.cleawing.ignite.akka.{IgniteConfig, IgniteExtension, IgniteExtensionImpl}
import com.typesafe.config.Config
import org.apache.ignite.cache.CacheMemoryMode
import org.apache.ignite.configuration.CollectionConfiguration
import com.cleawing.ignite.akka.IgniteConfig.ConfigOps

import scala.concurrent.duration.FiniteDuration

class IgniteBoundedMailbox(capacity: Int, pushTimeOut: FiniteDuration, _memoryMode: CacheMemoryMode)
  extends MailboxType with ProducesMessageQueue[IgniteBoundedQueueBasedMessageQueue] {

  import  org.apache.ignite.cache.CacheMode

  def this(settings: ActorSystem.Settings, config: Config) = this(
    config.getInt("mailbox-capacity"),
    config.getNanosDuration("mailbox-push-timeout-time"),
    config.getCacheMemoryMode("cache-memory-mode")
  )

  if (capacity < 0) throw new IllegalArgumentException("The capacity for IgniteBoundedMailbox can not be negative")
  if (pushTimeOut eq null) throw new IllegalArgumentException("The push time-out for IgniteBoundedMailbox can not be null")

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = {
    (owner, system) match {
      case (Some(o), Some(s)) =>
        implicit val ignite = IgniteExtension(s)
        val cfg = IgniteConfig.buildCollectionConfig(cacheMode = CacheMode.LOCAL, memoryMode = _memoryMode)
        new IgniteBoundedQueueBasedMessageQueue(capacity, pushTimeOut, o.path.toSerializationFormat, cfg, ignite)
    }
  }
}

class IgniteBoundedQueueBasedMessageQueue(capacity: Int,
                                          val pushTimeOut: FiniteDuration,
                                          queueName: String,
                                          cfg: CollectionConfiguration,
                                          ignite: IgniteExtensionImpl)
  extends BoundedQueueBasedMessageQueue {

  import akka.serialization.JavaSerializer.currentSystem

  final val queue = ignite.Collection.queue[Envelope](queueName, capacity, cfg)

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
