package com.cleawing.ignite.akka.transport

import akka.actor.{ExtendedActorSystem, Actor, InvalidActorNameException, Props}
import com.cleawing.ignite.akka.{IgniteConfig, Ignition}
import org.apache.ignite.{IgniteQueue, IgniteException}
import org.apache.ignite.cache.{CacheMemoryMode, CacheMode}

private[ignite] class SourceProxyActor(props: Props) extends Actor with Ignition {
  import com.cleawing.ignite.akka.transport.SourceProxyActor.ReadReplies

  private val services = ignite.Services(ignite.cluster().forRemotes())
  private val selfId = self.path.toStringWithoutAddress
  private val localNodeId = ignite.cluster().localNode().id().toString
  private val deploymentId = s"$localNodeId-$selfId"
  private var readQueue : IgniteQueue[(Any, String)] = _
  private var writeQueue : IgniteQueue[(Any, String)] = _

  override def preStart(): Unit = {
    if (exists(selfId)) {
      throw InvalidActorNameException(s"actor [$selfId] is already globally deployed")
    }
    try {
      val cfg = IgniteConfig.CollectionBuilder()
        .setCacheMode(CacheMode.PARTITIONED)
        .setMemoryMode(CacheMemoryMode.OFFHEAP_TIERED)
        .setBackups(2)
        .setCollocated(true)
        .build()
      readQueue = ignite.Collection.queue[(Any, String)](s"$deploymentId-reads", 0, cfg)
      writeQueue = ignite.Collection.queue[(Any, String)](s"$deploymentId-writes", 0, cfg)
      services.deployMultiple(selfId, ProxyActorService(props.clazz, props.args, deploymentId), 20, 1)
    } catch {
      case e: IgniteException => throw new IgniteException(s"actor [$selfId] could not remotely deployed due: ${e.getMessage}")
    }
    self ! ReadReplies
  }

  def receive = {
    case ReadReplies =>
      val resp = readQueue.poll()
      if (resp != null) {
        context.system.asInstanceOf[ExtendedActorSystem].provider.resolveActorRef(resp._2) ! resp._1
      }
      self ! ReadReplies
    case msg =>
      val s = if (sender ne Actor.noSender) sender() else context.system.deadLetters
      writeQueue.add((msg, s.path.toSerializationFormat))
  }

  override def postStop(): Unit = {
    services.cancel(selfId)
    readQueue.close()
    writeQueue.close()
  }

//  private def proxy : ActorService = {
//    services.proxy[ActorService](selfId, classOf[ActorService], sticky = false)
//  }

  private def exists(name: String) : Boolean = services.service[ProxyActorServiceImpl](name) != null
}

object SourceProxyActor {
  def apply(props: Props) : Props = Props(classOf[SourceProxyActor], props)
  case object ReadReplies
}
