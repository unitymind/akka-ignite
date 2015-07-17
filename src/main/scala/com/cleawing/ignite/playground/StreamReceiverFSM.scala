package com.cleawing.ignite.playground

import java.util

import akka.actor.{FSM, Props}
import com.cleawing.ignite.akka.Streaming._
import com.cleawing.ignite.akka.{Ignition, Streaming}
import org.apache.ignite.IgniteCache
import org.apache.ignite.stream.StreamReceiver

import scala.collection.JavaConversions._

class StreamReceiverFSM[K, V](name: String) extends FSM[State, Data]
  with Ignition with Streaming {
  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Start, Uninitialized) =>
      val streamer = ignite.dataStreamer[K, V](name)
      streamer.receiver(new StreamReceiver[K, V] {
        def receive(cache: IgniteCache[K, V], entries: util.Collection[util.Map.Entry[K, V]]) : Unit = {
          stateData match {
            case StreamEdge(_, subscribers) =>
              entries.foreach(e => fireSubcribers(Chunk[K, V](e.getKey, e.getValue), subscribers))
          }
        }
      })
      goto(Active) using StreamEdge(streamer)
  }

  when(Active) {
    case Event(Chunk(key, value), StreamEdge(streamer)) =>
      streamer.addData(key, value)
      stay()
    case Event(Flush, StreamEdge(streamer)) =>
      streamer.flush()
      stay()
    case Event(Pause, StreamEdge(streamer)) =>
      close(streamer)
      goto(Idle) using Uninitialized
    case Event(Stop, StreamEdge(streamer)) =>
      close(streamer)
      stop()
  }

  initialize()
}

object StreamReceiverFSM {
  def apply[K, V](cacheName: String) : Props = Props(classOf[StreamReceiverFSM[K,V]], cacheName)
}