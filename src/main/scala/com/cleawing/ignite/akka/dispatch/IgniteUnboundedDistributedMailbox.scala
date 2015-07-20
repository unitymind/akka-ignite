package com.cleawing.ignite.akka.dispatch

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.{MailboxType, MessageQueue, ProducesMessageQueue}
import com.cleawing.ignite.Implicits.ConfigOps
import com.cleawing.ignite.akka.dispatch.MessageQueues.IgniteUnboundedQueueBasedMessageQueue
import com.cleawing.ignite.akka.{IgniteConfig, IgniteExtension}
import com.typesafe.config.Config
import org.apache.ignite.cache.{CacheMemoryMode, CacheMode}

case class IgniteUnboundedDistributedMailbox(_memoryMode: CacheMemoryMode, _backups : Int)
  extends MailboxType with ProducesMessageQueue[IgniteUnboundedQueueBasedMessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = {
    this(config.getCacheMemoryMode("cache-memory-mode"), config.getInt("backups"))
  }

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = {
    (owner, system) match {
      case (Some(o), Some(s)) =>
        implicit val ignite = IgniteExtension(s)
        val cfg = IgniteConfig.CollectionBuilder()
          .setCacheMode(CacheMode.PARTITIONED)
          .setMemoryMode(_memoryMode)
          .setBackups(_backups)
          .build()
        new IgniteUnboundedQueueBasedMessageQueue(o.path.toStringWithoutAddress, cfg, ignite)
      case _ => throw new IllegalStateException("ActorRef and ActorSystem should be defined.")
    }
  }
}
