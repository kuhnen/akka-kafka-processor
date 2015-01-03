package com.github.kuhnen.unit.master

import akka.actor.ActorSystem
import akka.testkit.{EventFilter, TestActorRef, TestProbe}
import com.github.kuhnen.master.WorkersCoordinator
import com.github.kuhnen.master.WorkersCoordinator.{Topics, Work, WorkingTopics}
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor.TopicsAvailable
import com.github.kuhnen.{CommonActorSpec, LocalConf}
import scala.concurrent.duration._

/**
 * Created by kuhnen on 12/26/14.
 */


class WorkesCoordinatorSpec(_system: ActorSystem) extends CommonActorSpec(_system) {

  def this() = this(ActorSystem("CoordinatorSpec", LocalConf.conf))

  import com.github.kuhnen.master.WorkersCoordinator.RegisterWorker

  def coordinatorRef(name: String):TestActorRef[WorkersCoordinator] = TestActorRef[WorkersCoordinator](name = name)

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

  it should "log warning if there is no workers and exists topics" in {
    EventFilter.warning(occurrences = 1) intercept {
      val coordinator = coordinatorRef("no-worker")
      coordinator ! TopicsAvailable(Set("MyTopic"))
    }
  }

  it should "log warning if there is no topics available" in {
    EventFilter.warning(source = "akka://CoordinatorSpec/user/no-topics", occurrences = 1) intercept {
      val coordinator = coordinatorRef("no-topics")
      coordinator ! TopicsAvailable.empty
    }

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


  it should "ask workers which topics they are already working when receive avaialable topics and send the not working topics" in {
    withCoordinatorRegisteredWorker { (coordinatorRef, probe) =>
      val coordinator = coordinatorRef.underlyingActor
      coordinatorRef ! TopicsAvailable(Set("topic1", "topic2"))
      probe.expectMsg(WorkingTopics)
      probe.reply(Topics(Set("topic1")))
      probe.expectMsg(Work("topic2"))
    }
  }

  //TODO should send the topic to the worker with less load(NETWORK, CPU, MEMORY utilization)
  it should "it should send new topic to the workers with less topics" in {
    withCoordinatorRegistered2Workers { (coordinatorRef, probe1, probe2) =>
      val coordinator = coordinatorRef.underlyingActor
      coordinatorRef ! TopicsAvailable(Set("topic1", "topic2"))
      probe1.expectMsg(WorkingTopics)
      probe2.expectMsg(WorkingTopics)
      probe1.reply(Topics(Set("topic1")))
      probe2.reply(Topics.empty)
      probe2.expectMsg(Work("topic2"))
    }
  }

  //TODO  to be replaced bu y load balancing
  it should "send topic for worker with no topic and if there is only one worker wait x minutes to send the next topic" in {
    withCoordinatorRegisteredWorker { (coordinatorRef, probe) =>
      val coordinator = coordinatorRef.underlyingActor
      coordinatorRef ! TopicsAvailable(Set("topic1", "topic2", "topic3"))
      probe.expectMsg(WorkingTopics)
      probe.reply(Topics(Set("topic1")))
      probe.expectMsg(Work("topic2"))
      probe.expectNoMsg()
      coordinatorRef ! TopicsAvailable(Set("topic1", "topic2", "topic3"))
      probe.expectMsg(WorkingTopics)
      probe.reply(Topics(Set("topic1", "topic2")))
      probe.expectMsg(Work("topic3"))


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

  ignore should "ask workers how many topics they can handle" in {  }

  ignore should "deregister workers if there is no topic available" in {  }

  ignore should "use load balancing to send topics" in { }

}
