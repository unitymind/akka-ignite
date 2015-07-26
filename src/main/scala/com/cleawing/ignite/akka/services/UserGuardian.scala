package com.cleawing.ignite.akka.services

import java.util.UUID
import javax.cache.event.{EventType, CacheEntryEvent, CacheEntryUpdatedListener}

import akka.actor._
import com.cleawing.ignite.akka.Ignition
import org.apache.ignite.IgniteCache
import com.cleawing.ignite
import org.apache.ignite.cache.query.{ScanQuery, ContinuousQuery}
import java.lang.Iterable
import org.jsr166.ConcurrentHashMap8

import scala.collection.JavaConversions._

class UserGuardian extends Actor {
  private var localCache : IgniteCache[UUID, DeploymentActorService.Descriptor] = _
  private val deploymentMap = new ConcurrentHashMap8[String, IgniteCache[UUID, String]]
  private val watchers = new ConcurrentHashMap8[ActorRef, Set[String]]

  override def preStart(): Unit = {
    localCache = ignite.grid().Cache
      .getOrCreate[UUID, DeploymentActorService.Descriptor](DeploymentActorService.localCacheCfg.setName(s"akka_user_services"))
//    deploymentCache = ignite.grid().Cache
//      .getOrCreate[String, String](DeploymentActorService.deploymentCacheCfg.setName(Ignition.buildRemotePathString(self.path)))

    val qry = new ContinuousQuery[UUID, DeploymentActorService.Descriptor]
      .setInitialQuery(new ScanQuery[UUID, DeploymentActorService.Descriptor]())
      .setLocalListener(new CacheEntryUpdatedListener[UUID, DeploymentActorService.Descriptor] {
        override def onUpdated(events: Iterable[CacheEntryEvent[_ <: UUID, _ <: DeploymentActorService.Descriptor]]) : Unit = {
          events.foreach { event =>
            event.getEventType match {
              case EventType.CREATED => self ! UserGuardian.StartService(event.getKey, event.getValue)
              case EventType.REMOVED => self ! UserGuardian.StopService(event.getKey, event.getValue)
              case _ => // ignored
            }
          }
        }
    })

    localCache.query(qry).foreach{ entry =>
      self ! UserGuardian.StartService(entry.getKey, entry.getValue)
    }
  }

  def receive = {
    case UserGuardian.StartService(executionId, (props, serviceName, parent)) =>
      println((serviceName, parent))

      val parentRef = ignite.extended().provider.resolveActorRef(parent.get)
      if (parentRef != ignite.extended().provider.deadLetters) {
        val deploymentCache = deploymentMap.getOrElseUpdate(
          serviceName,
          resolveDeploymentCache(serviceName)
        )
        val ref = context.actorOf(props, executionId.toString)
        deploymentCache.put(executionId, Ignition.buildRemotePathString(ref.path))
        if (!watchers.containsKey(parentRef)) context.watch(parentRef)
        val services = watchers.getOrElseUpdate(parentRef, Set.empty[String])
        watchers.put(parentRef, services + serviceName)
      }
    case UserGuardian.StopService(executionId, (_, serviceName, _)) =>
      context.actorSelection(executionId.toString) ! PoisonPill
    case Terminated(parent) =>
      watchers.getOrDefault(parent, Set.empty[String]).foreach { service =>
        ignite.grid().Services().cancel(service)
      }
  }

  private def resolveDeploymentCache(serviceName: String) : IgniteCache[UUID, String] = {
    deploymentMap
      .getOrElseUpdate(
        serviceName,
        ignite.grid().Cache.getOrCreate[UUID, String](DeploymentActorService.deploymentCacheCfg.setName(serviceName))
      )
  }

}

object UserGuardian {
  def apply() : Props = Props[UserGuardian]
  case class StartService(executionId: UUID, descriptor: DeploymentActorService.Descriptor)
  case class StopService(executionId: UUID, descriptor: DeploymentActorService.Descriptor)
}
