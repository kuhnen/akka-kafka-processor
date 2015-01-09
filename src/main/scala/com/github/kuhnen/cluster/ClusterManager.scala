package com.github.kuhnen.cluster

import akka.actor._
import akka.cluster.Cluster
import akka.contrib.pattern.{ClusterClient, ClusterSingletonManager}
import akka.japi.Util._
import com.github.kuhnen.master.MasterActor._
import com.github.kuhnen.master.MasterWorkerProtocol.RegisterWorkerOnCluster
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor
import com.github.kuhnen.master.{MasterActor, WorkersCoordinator}
import akka.japi.Util.immutableSeq
import com.github.kuhnen.worker.KafkaWorker
import com.github.kuhnen.worker.executor.PlainTextTopicActor
import com.typesafe.config.{ConfigFactory, Config}
import com.typesafe.scalalogging.LazyLogging

/**
 * Created by kuhnen on 1/3/15.
 */

class ClusterManagerImpl(implicit val system: ActorSystem) extends ClusterManager

trait ClusterManager extends LazyLogging {

  import ClusterConfig._

  val system: ActorSystem
  lazy val cluster = Cluster

  def startMaster(role: String = "master"): ActorRef = {
    system.actorOf(ClusterSingletonManager.props(
      singletonProps = MasterActor.props(topicWatcherMaker, coordinatorMaker),
      singletonName = "active",
      terminationMessage = PoisonPill,
      role = Some(role),
      maxHandOverRetries = 6),
      "master")
  }

  def startWorker: ActorRef = {

    val conf = ClusterConfig.config.getConfig("processor")
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) =>
        logger.info(s"Initial contacts points: $addr")
        system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
        //system.actorSelection(RootActorPath(addr) / "user" / "master")
    }.toSet

    val executorMaker =(context: ActorRefFactory, name: String) => {
      context.actorOf(Props[PlainTextTopicActor], name)
    }

    val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    system.actorOf(KafkaWorker.props(clusterClient, executorMaker), "worker")

  }

  val topicWatcherMaker: ActorBuilder = { (context, optName) =>
    optName.map(name => context.actorOf(KafkaTopicWatcherActor.props(), name = name))
      .getOrElse(context.actorOf(KafkaTopicWatcherActor.props()))
  }

  val coordinatorMaker = {
    (context: ActorRefFactory, optName: Option[String]) =>
      optName.map(name => context.actorOf(WorkersCoordinator.props(), name = name))
        .getOrElse(context.actorOf(WorkersCoordinator.props()))
  }

  def startListener = system.actorOf(ClusterListener.props(), name = "listener")

}
