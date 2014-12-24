package com.github.kuhnen.master

import akka.actor._
import com.github.kuhnen.master.TopicWatcher.Topics
import com.github.kuhnen.master.WorkersCoordinator.{Work, RegisterWorker, WorkingTopics}
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor

/**
 * Created by kuhnen on 12/23/14.
 */

//class WorkersCoordinator extends LoggingFSM[WorkersCoordinator.State, WorkersCoordinator.Data] with ActorLogging {

/*class WorkersCoordinator extends LoggingFSM[WorkersCoordinator.State, WorkersCoordinator.Data] with ActorLogging {

  import WorkersCoordinator._
  val validTopics = Set("test")

  startWith(Idle, RegisteredWorkers(Set.empty))

  when(Idle) {
    case Event(RegisterWorker(worker), RegisteredWorkers(workers)) =>
      self ! RegisterWorkReceived
      goto(Registering) using Workers(worker, workers)
  }

  when(Registering) {
    case Event(RegisterWorkReceived, s) =>

    stay()


  }

  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }


  def askWorkersAboutTopics() = {
  //  workers.foreach(_ ! WorkingTopics)
  }

}
*/

class WorkersCoordinator extends Actor with ActorLogging {

  //var topicsByWorker =
  var availableTopics = Set.empty[String]
  var workers = Set.empty[ActorRef]

  override def receive = {

    case RegisterWorker(worker) => workers = workers + worker
    //  val workerTopics = topicsByWorker.getOrElse(worker, Set.empty)
    //  topicsByWorker = topicsByWorker + ((worker, workerTopics))

    case Topics(topics) => // delegateToWorkers(topics)
      availableTopics = availableTopics ++ topics
      context.become(waitingForWorkersTopics(workers.size, Map.empty.withDefaultValue(Set.empty)))

  }

  def waitingForWorkersTopics(remainingWorkers: Int, topicsByWorker: Map[ActorRef, Set[String]]): Receive = {

    case Topics(topics) if remainingWorkers == 1 =>

      val workersTopics = topicsByWorker.values.flatten.toSet
      val topicsToSend = workersTopics -- availableTopics
      log.debug("Topics to send to workers")
      delegateTopicsToWorkers(topicsToSend, topicsByWorker)


    case Topics(topics) =>
      val worker = sender()
      val workerTopics = topicsByWorker(worker) ++ topics
      val topicsByWorkerUpdated = topicsByWorker + ((worker, workerTopics))
      context.become(waitingForWorkersTopics(remainingWorkers - 1, topicsByWorkerUpdated))
  }

  def delegateTopicsToWorkers(topics: Set[String], topicsByWorker: Map[ActorRef, Set[String]]) = {
    val workersOrdered = topicsByWorker.toList.sortBy{ case(_, topics) => topics.size}.map{ case(actor,_)=> actor }
    val workersTopics = topics zip workersOrdered
    workersTopics.foreach { case (topic, actor)=>
      actor ! Work(topic)
    }

  }
}

object WorkersCoordinator {

  //EventReceived
  final case class RegisterWorker(worker: ActorRef)
  final case class Work(topic: String)
  case object RegisterWorkReceived

  sealed trait  State

  case object Idle extends State
  case object Registering extends State
  case object WaitingTopics extends State
  case object Coordinating extends State


  sealed trait Data

  final case class AvailableTopics(topics: Set[String]) extends Data
  final case class RegisteredWorkers(workers: Set[ActorRef]) extends Data
  final case class Workers(newWorker: ActorRef, availableWorkers: Set[ActorRef]) extends Data

  object WorkingTopics

  def props() = Props[WorkersCoordinator]

}
