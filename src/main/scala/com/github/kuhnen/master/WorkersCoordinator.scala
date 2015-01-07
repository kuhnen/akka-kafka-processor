package com.github.kuhnen.master

import akka.actor._
import akka.event.LoggingReceive
import com.github.kuhnen.master.WorkersCoordinator.{RegisterWorker, Topics, Work, WorkingTopics}
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor.TopicsAvailable


/**
 * Created by kuhnen on 12/23/14.
 */

//TODO refactor,  still thinking about how to make it as a FSM
class WorkersCoordinator extends Actor with Stash with ActorLogging {

  var availableTopics = Set.empty[String]
  var workers = Set.empty[ActorRef]
  var topicsByWorkerState = Map.empty[ActorRef, Set[String]].withDefaultValue(Set.empty)

  override def receive = idle

  def logCoordinatorState() = {
    log.info(s"Topics to Work: $availableTopics")
    val workersName = workers.map(_.path)
    log.info(s"Workers available: $workersName ")
  }

  def idle: Receive = LoggingReceive {

    case RegisterWorker(worker) => workers = workers + worker

    case TopicsAvailable(topics) if topics.isEmpty => logCoordinatorState()

    case TopicsAvailable(topics) if workers.size == 0 =>
      log.warning("There is no workers registered")
      logCoordinatorState()

    case TopicsAvailable(topics) =>
      availableTopics = topics
      val emptyTopicsByWorker = Map.empty[ActorRef, Set[String]].withDefaultValue(Set.empty)
      workers.foreach { _ ! WorkingTopics  }
      context.become(waitingForWorkersTopics(workers.size, emptyTopicsByWorker))
      logCoordinatorState()
  }

  def waitingForWorkersTopics(remainingWorkers: Int, topicsByWorker: Map[ActorRef, Set[String]]): Receive = {

    case Topics(topics) if remainingWorkers == 1 =>

      val updatedTopicsByWorker = updateTopicsByWorker(sender(), topics, topicsByWorker)
      val workersTopics = updatedTopicsByWorker.values.flatten.toSet
      val topicsToSend = availableTopics -- workersTopics
      log.info(s"New topics to send to workers: $topicsToSend")
      delegateTopicsToWorkers(topicsToSend, updatedTopicsByWorker)

    case Topics(topics) =>
      val updatedTopicsByWorker = updateTopicsByWorker(sender(), topics, topicsByWorker)
      context.become(waitingForWorkersTopics(remainingWorkers - 1, updatedTopicsByWorker))

    case msg: RegisterWorker => stash()
  }

  def updateTopicsByWorker(worker: ActorRef, topics: Set[String], topicsByWorker: Map[ActorRef, Set[String]]) = {
    val workerTopics = topicsByWorker(worker) ++ topics
    topicsByWorkerState =  topicsByWorker + ((worker, workerTopics))
    topicsByWorkerState

  }

  //TODO do load balancing
  def delegateTopicsToWorkers(topics: Set[String], topicsByWorker: Map[ActorRef, Set[String]]) = {
    val workersOrdered = topicsByWorker.toList.sortBy { case (_, topics) => topics.size}.map { case (actor, _) => actor}
    log.info(s"Delegating topics to workers: $workersOrdered")
    val workersTopics = topics zip workersOrdered
    workersTopics.foreach { case (topic, actor) => actor ! Work(topic)}
    //Wait for ok from workers???
    unstashAll()
    context.become(idle)
  }
}

object WorkersCoordinator {

  sealed trait CoordinatorProtocol

  final case class Topics(topics: Set[String]) extends CoordinatorProtocol

  object Topics {
    def empty = Topics(Set.empty)
  }

  //EventReceived
  final case class RegisterWorker(worker: ActorRef)

  final case class Work(topic: String)

  case object RegisterWorkReceived

  sealed trait State

  case object Idle extends State

  case object Registering extends State

  case object WaitingTopics extends State

  case object Coordinating extends State


  sealed trait Data

  final case class AvailableTopics(topics: Set[String]) extends Data

  //final case class WorkerRegisteredWorkers(workers: Set[ActorRef]) extends Data
  final case class Workers(availableWorkers: Set[ActorRef]) extends Data

  case object WorkingTopics

  def props() = Props[WorkersCoordinator]

}


//class WorkersCoordinator extends LoggingFSM[WorkersCoordinator.State, WorkersCoordinator.Data] with ActorLogging {
/*
class WorkersCoordinatorFSM extends LoggingFSM[WorkersCoordinator.State, WorkersCoordinator.Data] with ActorLogging {

  import WorkersCoordinator._

  startWith(Idle, Workers(Set.empty))

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
