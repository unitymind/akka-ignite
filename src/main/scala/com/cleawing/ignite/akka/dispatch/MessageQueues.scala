package com.cleawing.ignite.akka.dispatch

import akka.actor.{ExtendedActorSystem, ActorRef}
import akka.dispatch.{UnboundedQueueBasedMessageQueue, MessageQueue, Envelope, BoundedQueueBasedMessageQueue}
import com.cleawing.ignite.akka.IgniteExtension
import org.apache.ignite.configuration.CollectionConfiguration
import com.cleawing.ignite

import scala.concurrent.duration.FiniteDuration

object MessageQueues {
  class IgniteBoundedQueueBasedMessageQueue(
    capacity: Int, val pushTimeOut: FiniteDuration,
    queueName: String, cfg: CollectionConfiguration, system: ExtendedActorSystem) extends BoundedQueueBasedMessageQueue
  {
    import akka.serialization.JavaSerializer.currentSystem
    val grid = IgniteExtension(system)

    final val queue = grid.Collection.queue[Envelope](queueName, capacity, cfg)

    override def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
      currentSystem.withValue(system) { super.enqueue(receiver, handle) }
    }

    override def dequeue(): Envelope = {
      currentSystem.withValue(system) { super.dequeue() }
    }

    override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
      super.cleanUp(owner, deadLetters)
      queue.close()
    }
  }

  class IgniteUnboundedQueueBasedMessageQueue(queueName: String, cfg: CollectionConfiguration, system: ExtendedActorSystem)
    extends UnboundedQueueBasedMessageQueue {

    import akka.serialization.JavaSerializer.currentSystem
    val grid = IgniteExtension(system)

    final val queue = grid.Collection.queue[Envelope](queueName, 0, cfg)

    override def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
      currentSystem.withValue(system) { super.enqueue(receiver, handle) }
    }

    override def dequeue(): Envelope = {
      currentSystem.withValue(system) { super.dequeue() }
    }

    override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
      super.cleanUp(owner, deadLetters)
      queue.close()
    }
  }
}
