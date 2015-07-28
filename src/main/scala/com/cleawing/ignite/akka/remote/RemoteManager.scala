package com.cleawing.ignite.akka.remote

import java.util.UUID

import _root_.akka.actor.{Props, Actor}
import _root_.akka.stream.ActorMaterializer
import _root_.akka.stream.io.Framing
import _root_.akka.stream.scaladsl.{Flow, Tcp, Source}
import _root_.akka.stream.scaladsl.Tcp.{ServerBinding, IncomingConnection}
import _root_.akka.util.ByteString
import com.cleawing.ignite.akka.Ignition
import com.cleawing.ignite.akka.remote.Utils.HostPort
import org.apache.ignite.IgniteState
import org.apache.ignite.cache.{CacheRebalanceMode, CacheMode}
import org.apache.ignite.configuration.CacheConfiguration

import scala.concurrent.Future
import scala.collection.JavaConversions._

class RemoteManager extends Actor with Ignition {
  import RemoteManager._
  private val remoteCache = grid.Cache.getOrCreate[UUID, HostPort](remoteCacheCfg)

  override def preStart(): Unit = {
    implicit val materializer = ActorMaterializer()
    val hostPort = Utils.getRandomPortFor(grid.cluster().localNode().addresses().last).get
    val connections: Source[IncomingConnection, Future[ServerBinding]] = Tcp()(context.system).bind(hostPort._1, hostPort._2)

    connections runForeach { connection =>
      println(s"New connection from: ${connection.remoteAddress}")

      val echo = Flow[ByteString]
        .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
        .map(_.utf8String)
        .map(_ + "!!!\n")
        .map(ByteString(_))

      connection.handleWith(echo)
    }

    remoteCache.put(grid.localNodeId, hostPort)
  }

  def receive = Actor.emptyBehavior

  override def postStop(): Unit = {
    if (grid.state() == IgniteState.STARTED)
      remoteCache.remove(grid.localNodeId)
  }
}

object RemoteManager {
  def props() : Props  = Props[RemoteManager]
  val remoteCacheCfg = new CacheConfiguration[UUID, HostPort]()
    .setName("akka_remote")
    .setCacheMode(CacheMode.REPLICATED)
    .setRebalanceMode(CacheRebalanceMode.SYNC)
}
