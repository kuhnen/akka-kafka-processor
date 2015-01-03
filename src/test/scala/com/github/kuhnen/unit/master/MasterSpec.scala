package com.github.kuhnen.unit.master

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.testkit.TestProbe
import com.github.kuhnen.cluster.ClusterConfig
import com.github.kuhnen.master.MasterActor
import com.github.kuhnen.master.MasterActor.ActorBuilder
import com.github.kuhnen.master.MasterWorkerProtocol.RegisterWorkerOnCluster
import com.github.kuhnen.master.WorkersCoordinator.RegisterWorker
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor.TopicsAvailable
import com.github.kuhnen.CommonActorSpec

import scala.concurrent.duration._

/**
 * Created by kuhnen on 12/20/14.
 */


class MasterSpec(_system: ActorSystem) extends CommonActorSpec(_system) {

  def this() = this(ActorSystem(ClusterConfig.clusterName))

  val topicWatcherProbe = TestProbe()

  val coordinatorProbe = TestProbe()

  val topicWatcherMaker: ActorBuilder = { _: ActorRefFactory => topicWatcherProbe.ref}

  val coordinatorMaker: ActorBuilder = { _: ActorRefFactory => coordinatorProbe.ref}

  def createMaster() = system.actorOf(MasterActor.props(topicWatcherMaker, coordinatorMaker))

  //TODO only ask about topics again after coordinator finishes the task
  it should "ask the topic watcher every x seconds about topics" in {
    val master = createMaster()
    topicWatcherProbe.expectMsg(15 second, KafkaTopicWatcherActor.GetTopics)
    topicWatcherProbe.reply(TopicsAvailable.empty)
    coordinatorProbe.expectMsg(TopicsAvailable.empty)
    //topicWatcherProbe.expectMsg(5 second, KafkaTopicWatcherActor.GetTopics)
    //topicWatcherProbe.expectMsg(5 second, KafkaTopicWatcherActor.GetTopics)
  }

  it should "register a worker and send to coordinator" in {
    val master = createMaster()
    val workerProbe = TestProbe()
    master ! RegisterWorkerOnCluster(workerProbe.ref)
    coordinatorProbe.expectMsg(RegisterWorker(workerProbe.ref))
  }

  ignore should "supervise the topic watcher child" in {
    //val fuckedMaster = system.actorOf(MasterActor.props[ZKNotConnecting], name = "Master")
    //TestProbe()
    //    expectNoMsg(10 seconds)
  }

  ignore should "recover" in {}


}
