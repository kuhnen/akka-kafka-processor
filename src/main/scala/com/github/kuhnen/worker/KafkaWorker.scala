package com.github.kuhnen.worker

import akka.actor._
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.event.LoggingReceive
import com.github.kuhnen.master.MasterWorkerProtocol.RegisterWorkerOnCluster
import com.github.kuhnen.master.WorkersCoordinator.{Topics, Work, WorkingTopics}
import com.github.kuhnen.master.kafka.ZooKeeperConfig
import com.sclasen.akka.kafka.{AkkaConsumer, AkkaConsumerProps, CommitConfig}
import com.typesafe.config.{Config, ConfigFactory}
import kafka.message.MessageAndMetadata
import kafka.serializer.StringDecoder

import scala.concurrent.Future
import akka.pattern.pipe

import scala.util.Try

/**
 * Created by kuhnen on 12/17/14.
 */

object KafkaWorker {

  val singletonMasterPath = ConfigFactory.load().getConfig("processor.cluster.master").getString("path")
  def props(clusterClient: ActorRef, childMaker: (ActorRefFactory, String) => ActorRef): Props = Props(classOf[KafkaWorker], clusterClient, childMaker)
  case class Execute(topic: String)

}

object ConsumerConfig {

  import scala.concurrent.duration._
  val loaded = ConfigFactory.load()
  def getTopicConf(topicName: String) = {
    if (loaded.hasPath(s"kafka.topic.$topicName"))
      loaded.getConfig(s"kafka.topic.$topicName")
    else loaded.getConfig(s"kafka.topic.default")
  }

  def getTopicCommitInterval(topicName: String) = {
    Try(getTopicConf(topicName).getInt("commit.interval.seconds")).toOption.map(i => i seconds)
  }
  def getTopicCommitAfterMessageCount(topicName: String) =  {
    Try(getTopicConf(topicName).getInt("commit.afterCount")).toOption
  }

  def getTopicMaxInFlightPerStream(topicName: String) = {
    Try(getTopicConf(topicName).getInt("maxInFlightPerStream")).toOption.getOrElse(64)
  }

  def getTopicStreams(topicName: String) = {
    Try(getTopicConf(topicName).getInt("streams")).toOption.getOrElse(2)
  }

  lazy val groupPrefix= loaded.getString("kafka.group.prefix")
}

//trait KafkaConsumerFactory
//This actor is very simples, it only constructs a new AkkaKafkaConsumer, and the receives for the consumer
//The only logics to tests are the Maps,, but how could I mock the consumer?
class KafkaWorker(clusterClient: ActorRef, msgReceiverMaker: (ActorRefFactory, String) => ActorRef) extends Actor with ActorLogging {

  import com.github.kuhnen.worker.KafkaWorker._

  implicit val ec = context.system.dispatcher
  var executorByTopic = Map.empty[String, ActorRef]
  var consumerByName = Map.empty[String, AkkaConsumer[_,_]]
  val zkConnect = ZooKeeperConfig.hosts
  val groupPrefix = ConsumerConfig.groupPrefix

  def consumer(msgReceiver: ActorRef, topic: String, group: String, name: String) = {
    val commitConfig = CommitConfig(
      ConsumerConfig.getTopicCommitInterval(topic),
      ConsumerConfig.getTopicCommitAfterMessageCount(topic)
    )
    val streams = ConsumerConfig.getTopicStreams(topic)
    val maxInFlightPerStream = ConsumerConfig.getTopicMaxInFlightPerStream(topic)

   val props =  AkkaConsumerProps.forContext(
      context = context,
      zkConnect = zkConnect,
      topic = topic,
      group = group,
      streams = streams, //one per partition
      keyDecoder = new StringDecoder(),
      msgDecoder = new StringDecoder(),
      receiver = msgReceiver,
      msgHandler = kafkaMsgHandler,
      connectorActorName = Option(name),
      maxInFlightPerStream = maxInFlightPerStream,
      commitConfig = commitConfig
      //100 seconds

    )
    new AkkaConsumer(props)
  }
  
  val kafkaMsgHandler: MessageAndMetadata[String, String] => String = { msg => msg.message()}

  override def preStart(): Unit = sendToMaster(RegisterWorkerOnCluster(self))

  def receive = LoggingReceive {

    case Work(topic) if !executorByTopic.keys.toSet.contains(topic) =>
      val executor = msgReceiverMaker(context, topic)
      log.debug(s"Created actor $executor")
      executorByTopic = executorByTopic + ((topic, executor))
      val group = groupPrefix + "-" + topic
      val newConsumer: AkkaConsumer[String, String] = consumer(executor, topic, groupPrefix + "-" + topic, topic )
      consumerByName = consumerByName +((newConsumer.connector.path.name, newConsumer))
      log.debug(s"Created consumer $executor")
      log.debug(s"executors: $executorByTopic")
      log.debug(s"consumers: $consumerByName")
      newConsumer.start() pipeTo sender()

    case WorkingTopics => sender() ! Topics(executorByTopic.keys.toSet)

    case Work(topic) => log.warning(s"Something strange happened, this node is already working with $topic")


  }

  def sendToMaster(msg: Any): Unit = {
    clusterClient ! SendToAll(singletonMasterPath, msg)
  }

}

