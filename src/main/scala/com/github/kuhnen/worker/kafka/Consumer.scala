package com.github.kuhnen.worker.kafka

import akka.actor.{ActorContext, ActorRef}
import com.github.kuhnen.master.kafka.ZooKeeperConfig
import com.sclasen.akka.kafka.{AkkaConsumer, AkkaConsumerProps, CommitConfig}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import kafka.message.MessageAndMetadata
import kafka.serializer.StringDecoder

import scala.util.Try

/**
 * Created by kuhnen on 1/7/15.
 */

object Consumer extends LazyLogging {

  val zkConnect = ZooKeeperConfig.hosts

  val kafkaMsgHandler: MessageAndMetadata[String, String] => String = { msg => msg.message()}

  def apply(msgReceiver: ActorRef, topic: String, group: String, name: String)(implicit context: ActorContext) = {
    val commitConfig = CommitConfig(
      ConsumerConfig.getTopicCommitInterval(topic),
      ConsumerConfig.getTopicCommitAfterMessageCount(topic)
    )
    val streams = ConsumerConfig.getTopicStreams(topic)
    val maxInFlightPerStream = ConsumerConfig.getTopicMaxInFlightPerStream(topic)

    logger.debug(s"Commit config: $commitConfig")

    val props = AkkaConsumerProps.forContext(
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

  def getTopicCommitAfterMessageCount(topicName: String) = {
    Try(getTopicConf(topicName).getInt("commit.afterCount")).toOption
  }

  def getTopicMaxInFlightPerStream(topicName: String) = {
    Try(getTopicConf(topicName).getInt("maxInFlightPerStream")).toOption.getOrElse(64)
  }

  def getTopicStreams(topicName: String) = {
    Try(getTopicConf(topicName).getInt("streams")).toOption.getOrElse(2)
  }

  lazy val groupPrefix = loaded.getString("kafka.group.prefix")
}

