package com.cleawing.ignite.akka.services

import java.util.UUID
import javax.cache.Cache
import javax.cache.event.{EventType, CacheEntryEvent, CacheEntryUpdatedListener}

import akka.actor.{Props, Actor}
import akka.routing.{RoundRobinRoutingLogic, Router}

import com.cleawing.ignite
import com.cleawing.ignite.akka.Ignition
import org.apache.ignite.cache.CachePeekMode
import org.apache.ignite.cache.query.{QueryCursor, ScanQuery, ContinuousQuery}
import java.lang.Iterable
import scala.collection.JavaConversions._
import scala.concurrent.Future

class ServiceProxyRouter extends Actor {
  import ServiceProxyRouter._

  private val deploymentCache = ignite.grid().Cache.getOrCreate[UUID, String](
    DeploymentActorService.deploymentCacheCfg.setName(Ignition.buildRemotePathString(self.path))
  )

  private val qry = new ContinuousQuery[UUID, String]
    .setInitialQuery(new ScanQuery[UUID, String]())
    .setLocalListener(new CacheEntryUpdatedListener[UUID, String] {
    override def onUpdated(events: Iterable[CacheEntryEvent[_ <: UUID, _ <: String]]) : Unit = {
      events.foreach { event =>
        println(event)
        event.getEventType match {
          case EventType.CREATED => self ! AddInstance(event.getValue)
          case EventType.REMOVED => self ! RemoveInstance(event.getOldValue)
          case _ => // ignored
        }
      }
    }
  })

  private var router = Router(RoundRobinRoutingLogic())
  private var cursor : QueryCursor[Cache.Entry[UUID, String]] = _

  override def preStart(): Unit = {
    cursor = deploymentCache.query(qry)
    cursor.foreach{ entry =>
      self ! AddInstance(entry.getValue)
    }
  }

  def receive = {
    case AddInstance(path) =>
      val ref = ignite.extended().provider.resolveActorRef(path)
      if (ref != ignite.extended().provider.deadLetters) {
        router = router.addRoutee(ref)
      }

      println(s"Routees count after ADD: ${router.routees.size}")

    case RemoveInstance(path) =>
      val ref = ignite.extended().provider.resolveActorRef(path)
      if (ref != ignite.extended().provider.deadLetters) {
        router = router.removeRoutee(ref)
      }

      println(s"Routees count after REMOVE: ${router.routees.size}")

    case msg => router.route(msg, sender())
  }

  override def postStop(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    cursor.close()
    ignite.grid().Services().cancel(Ignition.buildRemotePathString(self.path))

    Future {
      while(deploymentCache.size(CachePeekMode.PRIMARY) != 0) Thread.sleep(100)
      ignite.grid().Cache.destroy(deploymentCache.getName)
    }
  }
}

object ServiceProxyRouter {
  def apply() : Props = Props[ServiceProxyRouter]

  case class AddInstance(path: String)
  case class RemoveInstance(path: String)
}



