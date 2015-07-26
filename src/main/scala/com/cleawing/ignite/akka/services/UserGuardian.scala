package com.cleawing.ignite.akka.services

import java.util.UUID
import javax.cache.event.{EventType, CacheEntryEvent, CacheEntryUpdatedListener}

import akka.actor._
import com.cleawing.ignite
import org.apache.ignite.cache.query.{ScanQuery, ContinuousQuery}
import java.lang.Iterable
import scala.collection.JavaConversions._
import DeploymentActorService._

class UserGuardian extends Actor {
  private val qry = new ContinuousQuery[UUID, Descriptor]
    .setInitialQuery(new ScanQuery[UUID, Descriptor]())
    .setLocalListener(new CacheEntryUpdatedListener[UUID, Descriptor] {
    override def onUpdated(events: Iterable[CacheEntryEvent[_ <: UUID, _ <: Descriptor]]) : Unit = {
      events.foreach { event =>
        event.getEventType match {
          case EventType.CREATED => self ! UserGuardian.StartService(event.getKey, event.getValue)
          case EventType.REMOVED => self ! UserGuardian.StopService(event.getKey, event.getOldValue)
          case _ => // ignored
        }
      }
    }
  })

  private val localCache = ignite.grid().Cache.getOrCreate[UUID, Descriptor](localCacheCfg.setName("akka_user_services"))

  override def preStart(): Unit = {
    localCache.query(qry).foreach{ entry =>
      self ! UserGuardian.StartService(entry.getKey, entry.getValue)
    }
  }

  def receive = {
    case UserGuardian.StartService(executionId, (props, _, _)) =>
      context.actorOf(props, executionId.toString)
    case UserGuardian.StopService(executionId, _) =>
      context.actorSelection(executionId.toString) ! PoisonPill
  }
}

object UserGuardian {
  def apply() : Props = Props[UserGuardian]
  case class StartService(executionId: UUID, descriptor: Descriptor)
  case class StopService(executionId: UUID, descriptor: Descriptor)
}
