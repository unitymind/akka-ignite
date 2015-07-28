package com.cleawing.ignite.akka.services

import java.util.UUID
import javax.cache.event.{EventType, CacheEntryEvent, CacheEntryUpdatedListener}

import akka.actor.{Actor, Props}
import com.cleawing.ignite
import com.cleawing.ignite.akka.Ignition
import com.cleawing.ignite.akka.services.DeploymentActorService._
import org.apache.ignite.cache.query.{ScanQuery, ContinuousQuery}
import scala.collection.JavaConversions._
import java.lang.Iterable

class ServicesGuardian extends Actor {
  context.actorOf(UserGuardian(), "user")
  
  def receive = Actor.emptyBehavior
}

object ServicesGuardian {
  def apply() : Props = Props[ServicesGuardian]
}

class UserGuardian extends Actor with Ignition {
  private val localCache = grid.Cache.getOrCreate[UUID, Descriptor](localCacheCfg.setName("akka_user_services"))

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

  override def preStart(): Unit = {
    localCache.query(qry).foreach{ entry =>
      self ! UserGuardian.StartService(entry.getKey, entry.getValue)
    }
  }

  def receive = {
    case UserGuardian.StartService(executionId, (props, serviceName, _)) =>
      context.system.actorSelection(serviceName) ! ServiceProxyRouter.Register(context.actorOf(props, executionId.toString))
    case UserGuardian.StopService(executionId, (_, serviceName,_)) =>
      context.child(executionId.toString).foreach(context.stop)
  }
}

object UserGuardian {
  def apply() : Props = Props[UserGuardian]
  case class StartService(executionId: UUID, descriptor: Descriptor)
  case class StopService(executionId: UUID, descriptor: Descriptor)
}
