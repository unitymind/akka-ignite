package com.cleawing.ignite.akka.dispatch

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch._
import com.cleawing.ignite.akka.{IgniteExtensionImpl, IgniteExtension}
import com.typesafe.config.Config
import org.apache.ignite.configuration.CollectionConfiguration

case class IgniteUnboundedMailbox(settings: ActorSystem.Settings, config: Config)
  extends MailboxType
  with ProducesMessageQueue[IgniteUnboundedQueueBasedMessageQueue] {

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = {
    (owner, system) match {
      case (Some(o), Some(s)) =>
        val ignite = IgniteExtension(s)
        val cfg = ignite.Collection.config()
        cfg.setCacheMode(org.apache.ignite.cache.CacheMode.LOCAL)
        cfg.setMemoryMode(org.apache.ignite.cache.CacheMemoryMode.OFFHEAP_TIERED)
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
