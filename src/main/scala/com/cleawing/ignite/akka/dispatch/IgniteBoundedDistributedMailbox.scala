package com.cleawing.ignite.akka.dispatch

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.{MailboxType, MessageQueue, ProducesMessageQueue}
import com.cleawing.ignite.Implicits.ConfigOps
import com.cleawing.ignite.Injector
import com.cleawing.ignite.akka.dispatch.MessageQueues.IgniteBoundedQueueBasedMessageQueue
import com.cleawing.ignite.akka.{IgniteExtension, IgniteConfig}
import com.typesafe.config.Config
import org.apache.ignite.cache.{CacheMemoryMode, CacheMode}

import scala.concurrent.duration.FiniteDuration

class IgniteBoundedDistributedMailbox(capacity: Int, pushTimeOut: FiniteDuration, _memoryMode: CacheMemoryMode, _backups : Int)
  extends MailboxType with ProducesMessageQueue[IgniteBoundedQueueBasedMessageQueue] {
  
  def this(settings: ActorSystem.Settings, config: Config) = this(
    config.getInt("mailbox-capacity"),
    config.getNanosDuration("mailbox-push-timeout-time"),
    config.getCacheMemoryMode("cache-memory-mode"),
    config.getInt("backups")
  )

  if (capacity < 0) throw new IllegalArgumentException("The capacity for IgniteBoundedMailbox can not be negative")
  if (pushTimeOut eq null) throw new IllegalArgumentException("The push time-out for IgniteBoundedMailbox can not be null")

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = {
    (owner, system) match {
      case (Some(o), Some(s)) =>
        implicit val ignite = Injector.grid()
        val cfg = IgniteConfig.CollectionBuilder()
          .setCacheMode(CacheMode.PARTITIONED)
          .setMemoryMode(_memoryMode)
          .setBackups(_backups)
          .setOffHeapMaxMemory(0)
          .build()
        new IgniteBoundedQueueBasedMessageQueue(capacity, pushTimeOut, o.path.toSerializationFormat, cfg)
      case _ => throw new IllegalStateException("ActorRef and ActorSystem should be defined.")
    }
  }
}
