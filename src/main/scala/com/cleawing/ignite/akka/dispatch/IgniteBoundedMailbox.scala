package com.cleawing.ignite.akka.dispatch

import akka.actor.{ActorSystem, ActorRef}
import akka.dispatch.{MessageQueue, ProducesMessageQueue, MailboxType}
import com.cleawing.ignite.akka.dispatch.MessageQueues.IgniteBoundedQueueBasedMessageQueue
import com.cleawing.ignite.akka.{IgniteConfig, IgniteExtension}
import com.typesafe.config.Config
import org.apache.ignite.cache.{CacheMode, CacheMemoryMode}
import com.cleawing.ignite.akka.IgniteConfig.ConfigOps

import scala.concurrent.duration.FiniteDuration

class IgniteBoundedMailbox(capacity: Int, pushTimeOut: FiniteDuration, _memoryMode: CacheMemoryMode)
  extends MailboxType with ProducesMessageQueue[IgniteBoundedQueueBasedMessageQueue] {

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
      case _ => throw new IllegalStateException("ActorRef and ActorSystem should be defined.")
    }
  }
}