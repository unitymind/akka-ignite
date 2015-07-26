package com.cleawing.ignite.akka.services

import java.util.UUID
import javax.cache.event.{EventType, CacheEntryEvent, CacheEntryUpdatedListener}

import akka.actor.{Props, Actor}
import akka.routing.{RoundRobinRoutingLogic, Router}
import org.apache.ignite.IgniteCache

import com.cleawing.ignite
import com.cleawing.ignite.akka.Ignition
import org.apache.ignite.cache.query.{ScanQuery, ContinuousQuery}
import java.lang.Iterable
import scala.collection.JavaConversions._

class ServiceProxyRouter extends Actor {
  import ServiceProxyRouter._

  private var deploymentCache : IgniteCache[UUID,String] = _
  private var router = Router(RoundRobinRoutingLogic())

  override def preStart(): Unit = {
    deploymentCache = ignite.grid().Cache.getOrCreate[UUID, String](
      DeploymentActorService.deploymentCacheCfg.setName(Ignition.buildRemotePathString(self.path))
    )

    val qry = new ContinuousQuery[UUID, String]
      .setInitialQuery(new ScanQuery[UUID, String]())
      .setLocalListener(new CacheEntryUpdatedListener[UUID, String] {
        override def onUpdated(events: Iterable[CacheEntryEvent[_ <: UUID, _ <: String]]) : Unit = {
          events.foreach { event =>
            println(event)
            event.getEventType match {
              case EventType.CREATED => self ! AddInstance(event.getValue)
              case EventType.REMOVED => self ! RemoveInstance(event.getValue)
              case _ => // ignored
            }
          }
        }
    })

    deploymentCache.query(qry).foreach{ entry =>
      self ! AddInstance(entry.getValue)
    }
  }

  def receive = {
    case AddInstance(path) =>
      val ref = ignite.extended().provider.resolveActorRef(path)
      if (ref != ignite.extended().provider.deadLetters) {
        router = router.addRoutee(ref)
      }

    case RemoveInstance(path) =>
      val ref = ignite.extended().provider.resolveActorRef(path)
      if (ref != ignite.extended().provider.deadLetters) {
        router = router.removeRoutee(ref)
      }

    case msg => router.route(msg, sender())
  }

  override def postStop(): Unit = {
    ignite.grid().Cache.destroy(deploymentCache.getName)
  }
}

object ServiceProxyRouter {
  def apply() : Props = Props[ServiceProxyRouter]
  case class AddInstance(path: String)
  case class RemoveInstance(path: String)
}


