package com.github.kuhnen.cluster

import akka.actor._
import akka.contrib.pattern.{ClusterClient, ClusterSingletonManager}
import akka.japi.Util._
import com.github.kuhnen.master.MasterActor._
import com.github.kuhnen.master.MasterWorkerProtocol.RegisterWorkerOnCluster
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor
import com.github.kuhnen.master.{MasterActor, WorkersCoordinator}
import akka.japi.Util.immutableSeq
import com.github.kuhnen.worker.KafkaWorker
import com.github.kuhnen.worker.executor.PlainTextTopicActor
import com.typesafe.scalalogging.LazyLogging

/**
 * Created by kuhnen on 1/3/15.
 */

trait ClusterManager extends LazyLogging {

  val system: ActorSystem

  def startMaster(role: String = "master"): Unit = {

    val topicWatcherMaker: ActorBuilder = { (context, optName) =>
      optName.map(name => context.actorOf(KafkaTopicWatcherActor.props(), name = name))
        .getOrElse(context.actorOf(KafkaTopicWatcherActor.props()))
    }

    val coordinatorMaker = {
      (context: ActorRefFactory, optName: Option[String]) =>
        optName.map(name => context.actorOf(WorkersCoordinator.props(), name = name))
          .getOrElse(context.actorOf(WorkersCoordinator.props()))
    }

    system.actorOf(ClusterSingletonManager.props(
      MasterActor.props(topicWatcherMaker, coordinatorMaker), "active", PoisonPill, Some(role), maxHandOverRetries = 6), "master")

  }

  def startListener(port: Int = 2551, role: String = "listener") = {

    system.actorOf(ClusterListener.props(), name = "listener")

  }

  def startWorker() = {

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
    val worker = system.actorOf(KafkaWorker.props(clusterClient, executorMaker), "worker")
    //clusterClient ! RegisterWorkerOnCluster(worker)
    //system.actorOf(Worker.props(clusterClient), "worker")

  }

}
