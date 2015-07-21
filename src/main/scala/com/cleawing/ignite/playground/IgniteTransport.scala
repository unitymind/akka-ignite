//package com.cleawing.ignite.playground
//
//import akka.actor._
//import akka.remote.transport.AssociationHandle.{ActorHandleEventListener, Disassociated, HandleEventListener}
//import akka.remote.transport.Transport.{AssociationEventListener, InboundAssociation}
//import akka.remote.transport.{AssociationHandle, Transport}
//import akka.util.ByteString
//import com.cleawing.ignite.akka.IgniteExtension
//import com.cleawing.ignite.playground.IgniteTransport.IgniteAssociationHandle
//import com.typesafe.config.Config
//import org.apache.ignite.events.{DiscoveryEvent, EventType}
//import org.apache.ignite.lang.IgnitePredicate
//import org.apache.ignite.spi.discovery.tcp.internal.TcpDiscoveryNode
//
//import scala.collection.JavaConversions._
//import scala.concurrent.{ExecutionContext, Future, Promise}
//
//class IgniteTransport(val settings: IgniteTransportSettings, val system: ExtendedActorSystem) extends Transport {
//  def this(system: ExtendedActorSystem, conf: Config) = this(new IgniteTransportSettings(conf), system)
//
//  override val schemeIdentifier = "ignite"
//  private val ignite = IgniteExtension(system)
//  private val associationListenerPromise: Promise[AssociationEventListener] = Promise()
//  private val localNode = ignite.cluster().localNode().asInstanceOf[TcpDiscoveryNode]
//  private val localAddress = Address(schemeIdentifier, system.name, localNode.addresses().toSeq.last, localNode.discoveryPort())
//
//  // TODO. Investigate how to use dedicated dispatcher
//  implicit val executionContext: ExecutionContext = system.dispatcher
//
//  private def discoveryListener(eventListener: AssociationEventListener) = new IgnitePredicate[DiscoveryEvent] {
//    def apply(evt: DiscoveryEvent) : Boolean = {
//      val remoteNode = evt.eventNode().asInstanceOf[TcpDiscoveryNode]
//      val associationHandle = new IgniteAssociationHandle(
//        localAddress,
//        Address(schemeIdentifier, system.name, remoteNode.addresses().toSeq.last, remoteNode.discoveryPort())
//      )
//      evt.`type` match {
//        case EventType.EVT_NODE_LEFT if evt.eventNode().id() != localNode.id() =>
//          println(s"LEFT: $evt")
//          associationHandle.readHandlerPromise.future.onSuccess {
//            case listener => listener.notify(Disassociated(AssociationHandle.Shutdown))
//          }
//        case EventType.EVT_NODE_FAILED if evt.eventNode().id() != localNode.id() =>
//          println(s"FAILED: $evt")
//        case EventType.EVT_NODE_JOINED if evt.eventNode().id() != localNode.id() =>
//
//          eventListener.notify(InboundAssociation(associationHandle))
//      }
//      true
//    }
//  }
//
//  // TODO. Investigate behavior
//  override def isResponsibleFor(address: Address): Boolean = true
//
//  // TODO. Really using for?
//  def maximumPayloadBytes: Int = 32000
//
//  // TODO. Investigate behavior
//  def listen: Future[(Address, Promise[AssociationEventListener])] = Future {
//    associationListenerPromise.future.onSuccess {
//      case listener =>
//        ignite.events().localListen(discoveryListener(listener), EventType.EVTS_DISCOVERY :_*)
//    }
//    (localAddress, associationListenerPromise)
//  }
//
//
//  // TODO. Investigate behavior
//  def associate(remoteAddress: Address): Future[AssociationHandle] = Future {
//    new IgniteAssociationHandle(localAddress, remoteAddress)
//  }
//
//  // TODO. Really using for?
//  def shutdown(): Future[Boolean] = Future { true }
//
//  // TODO. Seems useful
//  override def managementCommand(cmd: Any): Future[Boolean] = { Future.successful(false) }
//}
//
//object IgniteTransport {
//  class IgniteAssociationHandle(val localAddress: Address, val remoteAddress: Address) extends AssociationHandle {
//    import scala.concurrent.ExecutionContext.Implicits.global
//
//    def readHandlerPromise: Promise[HandleEventListener] = {
//      val prm = Promise[HandleEventListener]()
//      prm.future.onSuccess {
//        case ActorHandleEventListener(ref) =>
//          println(ref)
//      }
//      prm
//    }
//
//    // TODO. Use IgniteMessaging
//    def write(payload: ByteString): Boolean = {
//      println(s"WRITE: $payload")
//      true
//    }
//    def disassociate(): Unit = {
//      println("DIS CALLED")
//    }
//  }
//
//  class ReadHandler extends Actor {
//    def receive = {
//      case x => println(x)
//    }
//  }
//
//  object ReadHandler {
//    def apply() : Props = Props[ReadHandler]
//  }
//}
