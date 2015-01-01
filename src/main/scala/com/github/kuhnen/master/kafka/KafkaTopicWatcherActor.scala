package com.github.kuhnen.master.kafka

import akka.actor.{Actor, ActorLogging, Props}
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor.{GetTopics, TopicsAvailable}
import com.typesafe.config.ConfigFactory
import kafka.utils.{ZKStringSerializer, ZkUtils}
import org.I0Itec.zkclient.ZkClient

object ZooKeeperConfig {

  val zkConf = ConfigFactory.load().getConfig("kafka.zookeeper")
  val hosts = zkConf.getString("hosts")
  val sessionTimeOut = zkConf.getInt("session.timeout")
  val connectionTimeOut = zkConf.getInt("connection.timeout")

}

//trait  KafkaTopicWatcher extends TopicWatcher[ZkClient] {}

/**
 * Created by kuhnen on 12/16/14.
 */

class KafkaTopicWatcherActor extends TopicWatcher[ZkClient] with Actor with ActorLogging {

  import com.github.kuhnen.master.kafka.ZooKeeperConfig._

  override var client: ZkClient = _

  override def topics(): Set[String] = ZkUtils.getChildrenParentMayNotExist(client, ZkUtils.BrokerTopicsPath).sorted.toSet

  override def preStart() = {
    client = new ZkClient(hosts, sessionTimeOut, connectionTimeOut, ZKStringSerializer)
  }

  override def postStop() = {
    client.close()
  }

  override def receive = {
    case GetTopics => sender() ! TopicsAvailable(topics())
  }
}

object KafkaTopicWatcherActor {

  trait KafkaTopicsWatcherMessage

  case class TopicsAvailable(topics: Set[String])

  object TopicsAvailable {
    def empty = TopicsAvailable(Set.empty)
  }

  object GetTopics extends KafkaTopicsWatcherMessage

  def props() = Props[KafkaTopicWatcherActor]
}


object TopicWatcher {

  //type Topics = Set[String]

}

trait TopicWatcher[T] {

  var client: T

  def topics(): Set[String]

}
