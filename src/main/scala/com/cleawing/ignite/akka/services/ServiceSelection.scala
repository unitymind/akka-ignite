package com.cleawing.ignite.akka.services

import akka.actor._
import akka.util.Timeout
import com.cleawing.ignite

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

class ServiceSelection(name: String, sticky: Boolean = false)(implicit val context: ActorContext) {
  private val proxy = ignite.grid().Services().serviceProxy[DeploymentActorService](name, classOf[DeploymentActorService], sticky)
  private val proxyActor = context.actorOf(ServiceProxyActor(proxy))

  private def actorSelection() : ActorSelection = ignite.system().actorSelection(proxy.path())

  def !(msg: Any)(implicit sender: ActorRef = Actor.noSender) = tell(msg, sender)
  def forward(message: Any)(implicit context: ActorContext) = tell(message, context.sender())
  def tell(msg: Any, sender: ActorRef): Unit = proxyActor.tell(msg, sender)

  def resolveOne()(implicit timeout: Timeout): Future[ActorRef] = actorSelection().resolveOne()(timeout)
  def resolveOne(timeout: FiniteDuration): Future[ActorRef] = resolveOne()(timeout)
}

object ServiceSelection {
  def apply(name: String)(implicit context: ActorContext) : ServiceSelection = new ServiceSelection(name)
}

class ServiceProxyActor(proxy: DeploymentActorService) extends Actor {
  private var targetRef : ActorRef = _

  override def preStart() : Unit = {
    obtainActorRef()
  }

  def receive = {
    case Terminated(ref) => obtainActorRef()
    case msg =>
      targetRef.forward(msg)
  }

  private def obtainActorRef(): Unit = {
    targetRef = Await.result[ActorRef](ignite.system().actorSelection(proxy.path()).resolveOne(5.seconds), 5.seconds)
    context.watch(targetRef)
  }
}

object ServiceProxyActor {
  def apply(proxy: DeploymentActorService) : Props = Props(classOf[ServiceProxyActor], proxy)
}

