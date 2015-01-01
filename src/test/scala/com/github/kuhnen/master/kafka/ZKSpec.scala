package com.github.kuhnen.master.kafka

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.github.kuhnen.{CommonActorSpec, CommonSpecs}
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor.GetTopics
import org.scalatest.DoNotDiscover

import scala.concurrent.duration._

/**
 * Created by kuhnen on 12/20/14.
 */
class ZKConfSpec extends CommonSpecs {

  import com.github.kuhnen.master.kafka.ZooKeeperConfig._

  it should "retrieve the right configuration from file" in {

    hosts shouldBe "localhost:2181"
    sessionTimeOut shouldBe 30000
    connectionTimeOut shouldBe 30000

  }

}

//class KafkaTopicWatcherActorSpec(system: ActorSystem) extends TestKit(system) with ImplicitSender with CommonSpecs {

@DoNotDiscover
class KafkaTopicWatcherActorSpec(system: ActorSystem) extends CommonActorSpec(system) {

  def this() = this(ActorSystem("test"))

  val topicKafkaWatcherActor = system.actorOf(KafkaTopicWatcherActor.props())

  it should "reply the topics" in {
    topicKafkaWatcherActor ! GetTopics
    expectMsgType[Set[String]](10 seconds)
  }

}
