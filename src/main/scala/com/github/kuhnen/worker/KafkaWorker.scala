package com.github.kuhnen.worker

import akka.actor._
import akka.contrib.pattern.ClusterClient
import akka.event.LoggingReceive
import akka.pattern.pipe
import com.github.kuhnen.master.MasterWorkerProtocol.{RegisterWorkerOnCluster, Registered}
import com.github.kuhnen.master.WorkersCoordinator.{Topics, Work, WorkingTopics}
import com.github.kuhnen.worker.kafka.{Consumer, ConsumerConfig}
import com.sclasen.akka.kafka.AkkaConsumer
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
 * Created by kuhnen on 12/17/14.
 */

object KafkaWorker {

  val singletonMasterPath = ConfigFactory.load().getConfig("processor.cluster.master").getString("path")

  def props(clusterClient: ActorRef, childMaker: (ActorRefFactory, String) => ActorRef): Props = Props(classOf[KafkaWorker], clusterClient, childMaker)

  case class Execute(topic: String)

}


//trait KafkaConsumerFactory
//This actor is very simples, it only constructs a new AkkaKafkaConsumer, and the receives for the consumer
//The only logics to tests are the Maps,, but how could I mock the consumer?
class KafkaWorker(clusterClient: ActorRef, msgReceiverMaker: (ActorRefFactory, String) => ActorRef) extends Actor with ActorLogging {

  import com.github.kuhnen.worker.KafkaWorker._

  implicit val ec = context.system.dispatcher
  var executorByTopic = Map.empty[String, ActorRef]
  var consumerByName = Map.empty[String, AkkaConsumer[_, _]]
  val groupPrefix = ConsumerConfig.groupPrefix
  //It should be on preStart?
  val register = context.system.scheduler.schedule(10 seconds, 5 seconds) { sendToMaster(RegisterWorkerOnCluster(self)) }

  def receive =  registering

  def registering: Receive = {
    case Registered =>
      //TODO cancel registering  (NOT SO SIMPLE)  read the ticket
      //log.info(s"$self was successfully registered on $sender")
  //  register.cancel()
      context.become(registered)
  }

  def registered = LoggingReceive {

    case Work(topic) if !executorByTopic.keys.toSet.contains(topic) =>
      val executor = msgReceiverMaker(context, topic)
      log.debug(s"Created actor $executor")
      executorByTopic = executorByTopic + ((topic, executor))
      val group = groupPrefix + "-" + topic
      val newConsumer: AkkaConsumer[String, String] = Consumer(executor, topic, groupPrefix + "-" + topic, topic)(context)
      consumerByName = consumerByName + ((newConsumer.connector.path.name, newConsumer))
      log.debug(s"Created consumer $executor")
      log.debug(s"executors: $executorByTopic")
      log.debug(s"consumers: $consumerByName")
      newConsumer.start() pipeTo sender()

    case WorkingTopics => {
      log.debug(s"Working topics ${executorByTopic}")
      log.debug(s"Consumer by name ${consumerByName}")
      sender() ! Topics(executorByTopic.keys.toSet)

    }

    case Work(topic) => log.error(s"Something strange happened, this node is already working with $topic")

  }

  def sendToMaster(msg: Any): Unit = clusterClient ! ClusterClient.SendToAll(singletonMasterPath, msg)

}

