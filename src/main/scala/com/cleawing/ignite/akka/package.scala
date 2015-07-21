package com.cleawing.ignite

import _root_.akka.actor.{ActorContext, Props, ActorRef, Actor}
import com.cleawing.ignite.akka.remote.IgniteProxyActor

import org.apache.ignite.IgniteDataStreamer

package object akka {
  trait Ignition extends { this: Actor =>
    implicit final protected val ignite = IgniteExtension(context.system)

    def actorOf(props: Props) : ActorRef = {
      context.actorOf(IgniteProxyActor(props))
    }
    def actorOf(props: Props, name: String) : ActorRef = {
      context.actorOf(IgniteProxyActor(props), name)
    }
  }

  trait Streaming {
    import com.cleawing.ignite.akka.Streaming.Chunk

    def close(streamer: IgniteDataStreamer[Any, Any]) : Unit = {
      streamer.flush()
      streamer.close(true)
    }

    def fireSubcribers[K, V](chunk: Seq[Chunk[K,  V]], subscribers: Set[ActorRef]) : Unit = {
      subscribers.foreach(_ ! chunk)
    }
  }

  object Streaming {
    case object Start
    case object Pause
    case object Flush
    case object Stop
    case class Subscribe(ref: ActorRef)
    case class Unsubscribe(ref: ActorRef)
    case class Chunk[K, V](key: K, value: V)

    sealed trait State
    case object Idle extends State
    case object Active extends State

    sealed trait Data
    case object Uninitialized extends Data
    case class StreamEdge[K, V](streamer: IgniteDataStreamer[K, V], subscribers: Set[ActorRef] = Set.empty[ActorRef]) extends Data
  }
}
