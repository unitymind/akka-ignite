package com.cleawing.ignite.akka.services

import javax.cache.event.{EventType, CacheEntryEvent, CacheEntryUpdatedListener}

import akka.actor.{Actor, Props}
import com.cleawing.ignite.akka.Ignition
import com.cleawing.ignite.akka.services.DeploymentActorService._
import org.apache.ignite.IgniteException
import org.apache.ignite.cache.query.{ScanQuery, ContinuousQuery}
import org.apache.ignite.events.DiscoveryEvent
import org.apache.ignite.events.{EventType => IgniteEventType}
import org.apache.ignite.lang.IgnitePredicate
import scala.collection.JavaConversions._
import java.lang.Iterable

class ServicesGuardian extends Actor with Ignition {
  context.actorOf(UserGuardian.props(), "user")

  private val userDeploymentCache = grid.Cache.getOrCreate[GlobalDescriptor, NodeId](
    deploymentCacheCfg.setName(s"akka_user_services_deployment")
  ).withAsync()

  // TODO. What about threads?
  private val discoveryPredicate = new IgnitePredicate[DiscoveryEvent]() {
    override def apply(event: DiscoveryEvent) : Boolean = {
      try {
        val nodeDeploymentSet = grid.Collection.set[GlobalDescriptor](s"${event.eventNode().id()}-deployments", null)
        if (nodeDeploymentSet != null && ! nodeDeploymentSet.removed()) {
          userDeploymentCache.removeAll(nodeDeploymentSet.filter(_._1.contains("@")))
          nodeDeploymentSet.close()
        }
        // TODO. akka_system_services_deployment
      } catch {
        case e: IgniteException =>
          println(e)
      }

      true
    }
  }

  override def preStart(): Unit = {
    grid.events().localListen(discoveryPredicate, IgniteEventType.EVT_NODE_LEFT, IgniteEventType.EVT_NODE_FAILED)
  }
  
  def receive = Actor.emptyBehavior
}

object ServicesGuardian {
  def props() : Props = Props[ServicesGuardian]
}

class UserGuardian extends Actor with Ignition {
  private val localCache = grid.Cache.getOrCreate[ExecutionId, LocalDescriptor](localDeploymentCacheCfg.setName("akka_user_services_local")).withAsync()

  private val qry = new ContinuousQuery[ExecutionId, LocalDescriptor]
    .setInitialQuery(new ScanQuery[ExecutionId, LocalDescriptor]())
    .setLocalListener(new CacheEntryUpdatedListener[ExecutionId, LocalDescriptor] {
      override def onUpdated(events: Iterable[CacheEntryEvent[_ <: ExecutionId, _ <: LocalDescriptor]]) : Unit = {
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
    case UserGuardian.StartService(executionId, (props, _)) =>
      if(context.child(executionId.toString).isEmpty)
        context.actorOf(props, executionId.toString)
    case UserGuardian.StopService(executionId, _) =>
      context.child(executionId.toString).foreach(context.stop)
  }
}

object UserGuardian {
  def props() : Props = Props[UserGuardian]
  case class StartService(executionId: ExecutionId, descriptor: LocalDescriptor)
  case class StopService(executionId: ExecutionId, descriptor: LocalDescriptor)
}
