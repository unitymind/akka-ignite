package com.cleawing.ignite.playground

import akka.actor.{FSM, Props}
import com.cleawing.ignite.akka.Streaming._
import com.cleawing.ignite.akka.{Ignition, Streaming}

class DataStreamerFSM[K, V](name: String) extends FSM[State, Data]
  with Ignition with Streaming {

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Start, Uninitialized) =>
      goto(Active) using StreamEdge(ignite.dataStreamer[K, V](name))
  }

  when(Active) {
    case Event(Chunk(key, value), StreamEdge(streamer, _)) =>
      streamer.addData(key, value)
      stay()
    case Event(Flush, StreamEdge(streamer, _)) =>
      streamer.flush()
      stay()
    case Event(Pause, StreamEdge(streamer, _)) =>
      close(streamer)
      goto(Idle) using Uninitialized
    case Event(Stop, StreamEdge(streamer, _)) =>
      close(streamer)
      stop()
  }

  initialize()
}


object DataStreamerFSM {
  def apply[K, V](cacheName: String) : Props = Props(classOf[DataStreamerFSM[K,V]], cacheName)
}
