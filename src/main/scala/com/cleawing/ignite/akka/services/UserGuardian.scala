package com.cleawing.ignite.akka.services

import java.util.UUID
import javax.cache.event.{EventType, CacheEntryEvent, CacheEntryUpdatedListener}

import akka.actor._
import org.apache.ignite.IgniteCache
import com.cleawing.ignite
import org.apache.ignite.cache.query.{ScanQuery, ContinuousQuery}
import java.lang.Iterable
import org.jsr166.ConcurrentHashMap8

import scala.collection.JavaConversions._

class UserGuardian extends Actor {
  private var cache : IgniteCache[UUID, ActorService.ServiceDescriptor] = _
  private val watchers = new ConcurrentHashMap8[ActorRef, Set[String]]

  override def preStart(): Unit = {
    cache = ignite.grid().Cache
      .getOrCreate[UUID, ActorService.ServiceDescriptor](ActorService.cacheCfg.setName(s"akka_user_services"))

    val qry = new ContinuousQuery[UUID, ActorService.ServiceDescriptor]
      .setInitialQuery(new ScanQuery[UUID, ActorService.ServiceDescriptor]())
        .setLocalListener(new CacheEntryUpdatedListener[UUID, ActorService.ServiceDescriptor] {
        override def onUpdated(events: Iterable[CacheEntryEvent[_ <: UUID, _ <: ActorService.ServiceDescriptor]]) : Unit = {
          events.foreach { event =>
            event.getEventType match {
              case EventType.CREATED => self ! UserGuardian.StartService(event.getKey, event.getValue)
              case EventType.REMOVED => self ! UserGuardian.StopService(event.getKey)
              case _ => // ignored
            }
          }
        }
    })

    cache.query(qry).foreach{ entry =>
      println(entry)
      self ! UserGuardian.StartService(entry.getKey, entry.getValue)
    }
  }

  def receive = {
    case UserGuardian.StartService(executionId, (props, serviceName, parent)) =>
      val parentRef = ignite.extended().provider.resolveActorRef(parent.get)
      if (parentRef != ignite.extended().provider.deadLetters) {
        context.actorOf(props, executionId.toString)
        if (!watchers.containsKey(parentRef)) context.watch(parentRef)
        val services = watchers.getOrElseUpdate(parentRef, Set.empty[String])
        watchers.put(parentRef, services + serviceName)
      }
    case UserGuardian.StopService(executionId) =>
      context.actorSelection(executionId.toString) ! PoisonPill
    case Terminated(parent) =>
      watchers.getOrDefault(parent, Set.empty[String]).foreach { service =>
        ignite.grid().Services().cancel(service)
      }
  }

}

object UserGuardian {
  def apply() : Props = Props[UserGuardian]
  case class StartService(executionId: UUID, descriptor: ActorService.ServiceDescriptor)
  case class StopService(executionId: UUID)
}
