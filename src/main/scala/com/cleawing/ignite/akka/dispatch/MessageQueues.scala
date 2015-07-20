package com.cleawing.ignite.akka.dispatch

import akka.actor.ActorRef
import akka.dispatch.{UnboundedQueueBasedMessageQueue, MessageQueue, Envelope, BoundedQueueBasedMessageQueue}
import com.cleawing.ignite.akka.IgniteExtensionImpl
import org.apache.ignite.configuration.CollectionConfiguration

import scala.concurrent.duration.FiniteDuration

object MessageQueues {
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
}
