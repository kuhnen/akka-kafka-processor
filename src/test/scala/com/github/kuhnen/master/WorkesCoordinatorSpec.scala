package com.github.kuhnen.master

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe}
import com.github.kuhnen.{LocalConf, CommonActorSpec}
import com.github.kuhnen.master.WorkersCoordinator.{Topics, Work, WorkingTopics}
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor.TopicsAvailable
import com.typesafe.config.ConfigFactory

/**
 * Created by kuhnen on 12/26/14.
 */

object WorkersCoordinatorSpec {

  val confStr = """akka {
    actor.provider = "akka.actor.LocalActorRefProvider"
    loggers = [akka.event.slf4j.Slf4jLogger]
    loglevel = INFO
  }"""

  val conf = ConfigFactory.parseString(confStr)

}
class WorkesCoordinatorSpec(_system: ActorSystem) extends CommonActorSpec(_system) {

  def this() = this(ActorSystem("CoordinatorSpec", LocalConf.conf))

  import com.github.kuhnen.master.WorkersCoordinator.RegisterWorker

  def withCoordinatorRegisteredWorker(body: (TestActorRef[WorkersCoordinator], TestProbe) => Unit) = {
    val workProbe: TestProbe = TestProbe()
    val coordinatorRef: TestActorRef[WorkersCoordinator] = TestActorRef[WorkersCoordinator]
    coordinatorRef ! RegisterWorker(workProbe.ref)
    body(coordinatorRef, workProbe)
  }

  def withCoordinatorRegistered2Workers(body: (TestActorRef[WorkersCoordinator], TestProbe, TestProbe) => Unit) = {
    val workProbe: TestProbe = TestProbe()
    val workProbe2: TestProbe = TestProbe()
    val coordinatorRef: TestActorRef[WorkersCoordinator] = TestActorRef[WorkersCoordinator]
    coordinatorRef ! RegisterWorker(workProbe.ref)
    coordinatorRef ! RegisterWorker(workProbe2.ref)
    body(coordinatorRef, workProbe, workProbe2)
  }


  it should "register worker" in {
    val workProbe: TestProbe = TestProbe()
    val coordinatorRef: TestActorRef[WorkersCoordinator] = TestActorRef[WorkersCoordinator]
    val coordinator = coordinatorRef.underlyingActor
    coordinatorRef ! RegisterWorker(workProbe.ref)
    coordinator.workers should have size 1
    coordinatorRef ! RegisterWorker(workProbe.ref)
    coordinator.workers should have size 1
  }

  ignore should "ask workers which topics they are already working when receive avaialable topics " in {
    withCoordinatorRegisteredWorker { (coordinatorRef, probe) =>
      val coordinator = coordinatorRef.underlyingActor
      coordinatorRef ! Topics(Set("topic1", "topic2"))
      probe.expectMsg(WorkingTopics)
      probe.reply(Topics(Set("topic1")))
      probe.expectMsg(Work("topic2"))
    }
  }

  //TODO  to be replaced bu y load balancing
  it should "send topic for worker with no topic" in {
    withCoordinatorRegistered2Workers { (coordinatorRef, probe, probe2) =>
      val coordinator = coordinatorRef.underlyingActor
      coordinator.workers should have size 2
      coordinatorRef ! TopicsAvailable(Set("topic1", "topic2"))
      probe.expectMsg(WorkingTopics)
      probe2.expectMsg(WorkingTopics)
      probe.reply(Topics(Set("topic1")))
      probe2.reply(Topics.empty)
      probe2.expectMsg(Work("topic2"))
      probe.expectNoMsg()

    }
  }

  it should "be able to register new workers after sending messages to workers" in {
    withCoordinatorRegisteredWorker { (coordinatorRef, probe) =>
      val coordinator = coordinatorRef.underlyingActor
      coordinatorRef ! TopicsAvailable(Set("topic1", "topic2"))
      val probe2 = TestProbe()
      coordinatorRef ! RegisterWorker(probe2.ref)
      coordinator.workers should have size 1
      probe.expectMsg(WorkingTopics)
      probe.reply(Topics(Set("topic1")))
      probe.expectMsg(Work("topic2"))
      coordinator.workers should have size 2

    }

  }

  ignore should "be able to recover if a node is down" in {}

  ignore should "ask workers how many topics they can handle" in {

  }

  ignore should "use load balancing to send topics" in {

  }

}
