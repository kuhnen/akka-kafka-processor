package com.github.kuhnen.master

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe}
import com.github.kuhnen.CommonActorSpec
import com.github.kuhnen.master.TopicWatcher.Topics
import com.typesafe.config.ConfigFactory

/**
 * Created by kuhnen on 12/26/14.
 */
class WorkesCoodinatorSpec(_system: ActorSystem) extends CommonActorSpec(_system) {


  def this() = this(ActorSystem("CoordinatorSpec", ConfigFactory.parseString("""
    akka.actor.provider = akka.actor.LocalActorRefProvider""")))

  import WorkersCoordinator.RegisterWorker

  def withCoordinatorRegisteredWorker(body: (TestActorRef[WorkersCoordinator], TestProbe) => Unit) = {
    val workProbe: TestProbe = TestProbe()
    val coordinatorRef: TestActorRef[WorkersCoordinator] = TestActorRef[WorkersCoordinator]
    coordinatorRef ! RegisterWorker(workProbe.ref)
    body(coordinatorRef, workProbe)


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

  it should "ask workers which topics they are already working when receive topics " in {
    withCoordinatorRegisteredWorker{ (coordinatorRef, probe) =>
      val coordinator = coordinatorRef.underlyingActor
      coordinatorRef ! Topics(Set("topic1", "topic2"))
    }


  }

  it should "ask workers how many topics they can handle" in {

  }

}
