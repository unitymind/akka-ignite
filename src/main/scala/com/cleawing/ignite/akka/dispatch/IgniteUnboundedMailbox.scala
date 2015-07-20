package com.cleawing.ignite.akka.dispatch

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.{MessageQueue, ProducesMessageQueue, MailboxType}
import com.cleawing.ignite.akka.dispatch.MessageQueues.IgniteUnboundedQueueBasedMessageQueue
import com.cleawing.ignite.akka.{IgniteConfig, IgniteExtension}
import com.typesafe.config.Config
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
      case _ => throw new IllegalStateException("ActorRef and ActorSystem should be defined.")
    }
  }
}