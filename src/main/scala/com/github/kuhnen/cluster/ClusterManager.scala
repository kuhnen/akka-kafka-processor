package com.github.kuhnen.cluster

import akka.actor._
import akka.contrib.pattern.{ClusterClient, ClusterSingletonManager}
import akka.japi.Util._
import com.github.kuhnen.master.MasterActor._
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

  def startMaster(port: Int, role: String = "master"): Unit = {

    val topicWatcherMaker: ActorBuilder = { context => context.actorOf(KafkaTopicWatcherActor.props())}
    val coordinatorMaker: ActorBuilder = { context => context.actorOf(WorkersCoordinator.props())}
    system.actorOf(ClusterSingletonManager.props(MasterActor.props(topicWatcherMaker, coordinatorMaker), "active", PoisonPill, Some(role)), "master")

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
    }.toSet

    val executorMaker =(context: ActorRefFactory, name: String) => {
      context.actorOf(Props[PlainTextTopicActor], name)
    }
    val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    system.actorOf(KafkaWorker.props(clusterClient, executorMaker), "worker")
    //system.actorOf(Worker.props(clusterClient), "worker")

  }

}
