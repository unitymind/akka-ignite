package com.cleawing.ignite.akka.remote

import akka.actor.{ExtendedActorSystem, Actor, InvalidActorNameException, Props}
import com.cleawing.ignite.akka.{IgniteConfig, Ignition}
import org.apache.ignite.{IgniteQueue, IgniteException}
import org.apache.ignite.cache.{CacheMemoryMode, CacheMode}
import scala.concurrent.duration._

private[ignite] class IgniteProxyActor(props: Props) extends Actor with Ignition {
  import com.cleawing.ignite.Implicits._
  import com.cleawing.ignite.akka.remote.IgniteProxyActor.ReadResponses
  import context.dispatcher

  private val services = ignite.Services(ignite.cluster().forRemotes())
  private val deploymentName = self.path.toStringWithoutAddress
  private var queue : IgniteQueue[(String, Any)] = _
  private val localNodeId = ignite.cluster().localNode().id().toString

  override def preStart(): Unit = {
    if (exists(deploymentName)) {
      throw InvalidActorNameException(s"actor [$deploymentName] is already globally deployed")
    }
    try {
      services.deployMultiple(deploymentName, IgniteActorService(props.clazz, props.args), 1, 0)
    } catch {
      case e: IgniteException => throw new IgniteException(s"actor [$deploymentName] could not remotely deployed due: ${e.getMessage}")
    }

    val cfg = IgniteConfig.CollectionBuilder()
      .setCacheMode(CacheMode.PARTITIONED)
      .setMemoryMode(CacheMemoryMode.OFFHEAP_TIERED)
//      .setBackups(1)
      .build()
    queue = ignite.Collection.queue[(String, Any)](s"$localNodeId-$deploymentName", 0, cfg)
    self ! ReadResponses
  }

  def receive = {
    case ReadResponses =>
      val resp = queue.poll()
      if (resp != null) {
        context.system.asInstanceOf[ExtendedActorSystem].provider.resolveActorRef(resp._1) ! resp._2
      }
      context.system.scheduler.scheduleOnce(50.milliseconds, self, ReadResponses)
    case msg =>
      proxy ! ProxyEnvelope(msg, sender(), s"$localNodeId-$deploymentName", ignite.actorSystem)
  }

  override def postStop(): Unit = {
    services.cancel(deploymentName)
    queue.close()
  }

  private def proxy : IgniteActorService = {
    services.proxy[IgniteActorService](deploymentName, classOf[IgniteActorService], sticky = false)
  }

  private def exists(name: String) : Boolean = services.service[IgniteActorServiceImpl](name) != null
}

object IgniteProxyActor {
  def apply(props: Props) : Props = Props(classOf[IgniteProxyActor], props)
  case object ReadResponses
}
