package com.cleawing.ignite.akka.dispatch

import akka.actor.{ExtendedActorSystem, ActorRef}
import akka.dispatch.{UnboundedQueueBasedMessageQueue, MessageQueue, Envelope, BoundedQueueBasedMessageQueue}
import com.cleawing.ignite.{Injector, IgniteGrid}
import com.cleawing.ignite.akka.IgniteExtensionImpl
import org.apache.ignite.configuration.CollectionConfiguration

import scala.concurrent.duration.FiniteDuration

object MessageQueues {
  class IgniteBoundedQueueBasedMessageQueue(
    capacity: Int, val pushTimeOut: FiniteDuration,
    queueName: String, cfg: CollectionConfiguration) extends BoundedQueueBasedMessageQueue
  {
    import akka.serialization.JavaSerializer.currentSystem

    final val queue = Injector.grid().Collection.queue[Envelope](queueName, capacity, cfg)

    override def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
      currentSystem.withValue(Injector.actorSystem().asInstanceOf[ExtendedActorSystem]) { super.enqueue(receiver, handle) }
    }

    override def dequeue(): Envelope = {
      currentSystem.withValue(Injector.actorSystem().asInstanceOf[ExtendedActorSystem]) { super.dequeue() }
    }

    override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
      super.cleanUp(owner, deadLetters)
      queue.close()
    }
  }

  class IgniteUnboundedQueueBasedMessageQueue(queueName: String, cfg: CollectionConfiguration)
    extends UnboundedQueueBasedMessageQueue {

    import akka.serialization.JavaSerializer.currentSystem

    final val queue = Injector.grid().Collection.queue[Envelope](queueName, 0, cfg)

    override def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
      currentSystem.withValue(Injector.actorSystem().asInstanceOf[ExtendedActorSystem]) { super.enqueue(receiver, handle) }
    }

    override def dequeue(): Envelope = {
      currentSystem.withValue(Injector.actorSystem().asInstanceOf[ExtendedActorSystem]) { super.dequeue() }
    }

    override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
      super.cleanUp(owner, deadLetters)
      queue.close()
    }
  }
}
