package com.github.kuhnen.master

/**
 * Created by kuhnen on 12/17/14.
 */

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor._
import akka.contrib.pattern.{ClusterReceptionistExtension, DistributedPubSubExtension}
import com.github.kuhnen.master.MasterWorkerProtocol.RegisterWorkerOnCluster
import com.github.kuhnen.master.WorkersCoordinator.RegisterWorker
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor.TopicsAvailable

import scala.concurrent.duration.Deadline
import scala.reflect.ClassTag


object MasterActor {

  type ActorBuilder = ActorRefFactory => ActorRef
  //def props[T <: Actor](implicit tag: ClassTag[T]) = Props(new MasterActor[T])
  def props(topicWatcherMaker: ActorRefFactory => ActorRef,
            coordinatorActorMaker: ActorRefFactory => ActorRef) = Props(classOf[MasterActor], topicWatcherMaker, coordinatorActorMaker)

}

object MasterWorkerProtocol {

  case class RegisterWorkerOnCluster(work: ActorRef)

  //  object WorkingTopics

}

import scala.concurrent.duration._

//class MasterActor[T <: Actor](implicit tags: ClassTag[T]) extends Actor with ActorLogging {
//class MasterActor(topicsWatcher: Props, coordinator: Props) extends Actor with ActorLogging {
  class MasterActor(topicWatcherMaker: ActorRefFactory => ActorRef,
                    coordinatorActorMaker: ActorRefFactory => ActorRef) extends Actor with ActorLogging {

  //type TopicWatcherType = T
  implicit val ec = context.system.dispatcher
  val mediator = DistributedPubSubExtension(context.system).mediator
  ClusterReceptionistExtension(context.system).registerService(self)

  var topicWatcher: ActorRef = _
  var coordinatorActor: ActorRef = _
  var topicsCancellable: Cancellable = _
  //var topicsAvailable: Set[String] = Set.empty

  override val supervisorStrategy = OneForOneStrategy(
    maxNrOfRetries = 1,
    withinTimeRange = 10 seconds,
    loggingEnabled = true) {
    case ActorInitializationException(actor, message, throwable) =>
      log.error(s"$actor not able to Initialize.\n Message: $message. \n Error: ${throwable.getMessage}")
      Restart
    case e =>
      log.error("Unexpected failure: {}", e.getMessage)
      Escalate
  }

  override def preStart(): Unit = {

    //topicWatcher = context.actorOf(topicsWatcher, name = "kafka-topic-watcher")
    topicWatcher = topicWatcherMaker(context)
    topicsCancellable = context.system.scheduler.schedule(5 seconds, 2 seconds, topicWatcher, KafkaTopicWatcherActor.GetTopics)
    //coordinatorActor = context.actorOf(WorkersCoordinator.props(), name = "WorkersCoodinator")
    coordinatorActor = coordinatorActorMaker(context)

  }

  override def postStop(): Unit = {
    topicsCancellable.cancel()
  }

  def receive = {

    case TopicsAvailable(topics) =>
      log.info(s"Master knows about these topics: $topics")
      topicsCancellable.cancel()
      coordinatorActor ! TopicsAvailable(topics)

    case RegisterWorkerOnCluster(worker) =>
      //workers = workers + worker
      log.info(s"Registering worker $worker")
      coordinatorActor ! RegisterWorker(worker)


  }

  //class Master(workTimeout: FiniteDuration) extends PersistentActor with ActorLogging {

  //import WorkState._


  /*
    // persistenceId must include cluster role to support multiple masters
    override def persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("backend-")) match {
      case Some(role) ⇒ role + "-master"
      case None       ⇒ "master"
    }

    // workers state is not event sourced
    private var workers = Map[String, WorkerState]()

    // workState is event sourced
    private var workState = WorkState.empty

    import context.dispatcher
    val cleanupTask = context.system.scheduler.schedule(workTimeout / 2, workTimeout / 2,
      self, CleanupTick)

    override def postStop(): Unit = cleanupTask.cancel()

    override def receiveRecover: Receive = {
      case event: WorkDomainEvent =>
        // only update current state by applying the event, no side effects
        workState = workState.updated(event)
        log.info("Replayed {}", event.getClass.getSimpleName)
    }

    override def receiveCommand: Receive = {
      case MasterWorkerProtocol.RegisterWorker(workerId) =>
        if (workers.contains(workerId)) {
          workers += (workerId -> workers(workerId).copy(ref = sender()))
        } else {
          log.info("Worker registered: {}", workerId)
          workers += (workerId -> WorkerState(sender(), status = Idle))
          if (workState.hasWork)
            sender() ! MasterWorkerProtocol.WorkIsReady
        }

      case MasterWorkerProtocol.WorkerRequestsWork(workerId) =>
        if (workState.hasWork) {
          workers.get(workerId) match {
            case Some(s @ WorkerState(_, Idle)) =>
              val work = workState.nextWork
              persist(WorkStarted(work.workId)) { event =>
                workState = workState.updated(event)
                log.info("Giving worker {} some work {}", workerId, work.workId)
                workers += (workerId -> s.copy(status = Busy(work.workId, Deadline.now + workTimeout)))
                sender() ! work
              }
            case _ =>
          }
        }

      case MasterWorkerProtocol.WorkIsDone(workerId, workId, result) =>
        // idempotent
        if (workState.isDone(workId)) {
          // previous Ack was lost, confirm again that this is done
          sender() ! MasterWorkerProtocol.Ack(workId)
        } else if (!workState.isInProgress(workId)) {
          log.info("Work {} not in progress, reported as done by worker {}", workId, workerId)
        } else {
          log.info("Work {} is done by worker {}", workId, workerId)
          changeWorkerToIdle(workerId, workId)
          persist(WorkCompleted(workId, result)) { event ⇒
            workState = workState.updated(event)
            mediator ! DistributedPubSubMediator.Publish(ResultsTopic, WorkResult(workId, result))
            // Ack back to original sender
            sender ! MasterWorkerProtocol.Ack(workId)
          }
        }

      case MasterWorkerProtocol.WorkFailed(workerId, workId) =>
        if (workState.isInProgress(workId)) {
          log.info("Work {} failed by worker {}", workId, workerId)
          changeWorkerToIdle(workerId, workId)
          persist(WorkerFailed(workId)) { event ⇒
            workState = workState.updated(event)
            notifyWorkers()
          }
        }

      case work: Work =>
        // idempotent
        if (workState.isAccepted(work.workId)) {
          sender() ! Master.Ack(work.workId)
        } else {
          log.info("Accepted work: {}", work.workId)
          persist(WorkAccepted(work)) { event ⇒
            // Ack back to original sender
            sender() ! Master.Ack(work.workId)
            workState = workState.updated(event)
            notifyWorkers()
          }
        }

      case CleanupTick =>
        for ((workerId, s @ WorkerState(_, Busy(workId, timeout))) ← workers) {
          if (timeout.isOverdue) {
            log.info("Work timed out: {}", workId)
            workers -= workerId
            persist(WorkerTimedOut(workId)) { event ⇒
              workState = workState.updated(event)
              notifyWorkers()
            }
          }
        }
    }

    def notifyWorkers(): Unit =
      if (workState.hasWork) {
        // could pick a few random instead of all
        workers.foreach {
          case (_, WorkerState(ref, Idle)) => ref ! MasterWorkerProtocol.WorkIsReady
          case _                           => // busy
        }
      }

    def changeWorkerToIdle(workerId: String, workId: String): Unit =
      workers.get(workerId) match {
        case Some(s @ WorkerState(_, Busy(`workId`, _))) ⇒
          workers += (workerId -> s.copy(status = Idle))
        case _ ⇒
        // ok, might happen after standby recovery, worker state is not persisted
      }

    // TODO cleanup old workers
    // TODO cleanup old workIds, doneWorkIds
  */
}