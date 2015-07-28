package com.cleawing.ignite.akka.dispatch

import akka.actor.{ExtendedActorSystem, ActorRef, ActorSystem}
import akka.dispatch.{MessageQueue, ProducesMessageQueue, MailboxType}
import com.cleawing.ignite.akka.dispatch.MessageQueues.IgniteUnboundedQueueBasedMessageQueue
import com.cleawing.ignite.akka.{IgniteExtension, IgniteConfig}
import com.typesafe.config.Config
import org.apache.ignite.cache.{CacheMode, CacheMemoryMode}
import com.cleawing.ignite.Implicits.ConfigOps

case class IgniteUnboundedMailbox(_memoryMode: CacheMemoryMode)
  extends MailboxType with ProducesMessageQueue[IgniteUnboundedQueueBasedMessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = {
    this(config.getCacheMemoryMode("cache-memory-mode"))
  }

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = {
    (owner, system) match {
      case (Some(o), Some(s)) =>
        implicit val ignite = IgniteExtension(s)
        val cfg = IgniteConfig.CollectionBuilder()
          .setCacheMode(CacheMode.LOCAL)
          .setMemoryMode(_memoryMode)
          .setOffHeapMaxMemory(0)
          .build()
        new IgniteUnboundedQueueBasedMessageQueue(o.path.toStringWithoutAddress, cfg, s.asInstanceOf[ExtendedActorSystem])
      case _ => throw new IllegalStateException("ActorRef and ActorSystem should be defined.")
    }
  }
}
