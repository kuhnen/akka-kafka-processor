package com.github.kuhnen.master

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.testkit.TestProbe
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor.{TopicsAvailable, GetTopics}
import com.github.kuhnen.{ClusterConfig, LocalConf, CommonActorSpec}
import com.github.kuhnen.master.MasterActor.ActorBuilder
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor
import org.scalatest.DoNotDiscover

import scala.concurrent.duration._

/**
 * Created by kuhnen on 12/20/14.
 */
/*
class ZKNotConnecting extends KafkaTopicWatcherActor {

  //override var client = null

  var numberOfTries = 0

  override def preStart(): Unit = {
    numberOfTries = numberOfTries + 1
    log.info(s"Number of tries: $numberOfTries")
    throw new RuntimeException("BUMM!!! Topic actor exploded when trying to connect")
  }

  override def receive = {
    case x => log.info(s"RECEIVING  MESSAGE: $x")
  }
}
*/

class MasterSpec(_system: ActorSystem) extends CommonActorSpec(_system) {


  def this() = this(ActorSystem(ClusterConfig.clusterName))


  //implicit val _system = system
  val topicWatcherProbe = TestProbe()
  val coordinatorProbe = TestProbe()
  def createMaster() = system.actorOf(MasterActor.props(topicWatcherMaker , coordinatorMaker))

  val topicWatcherMaker: ActorBuilder = { _: ActorRefFactory => topicWatcherProbe.ref }

  val coordinatorMaker: ActorBuilder = { _: ActorRefFactory => coordinatorProbe.ref }


  it should "ask the topic watcher every x seconds about topics" in {
    val master = createMaster()
    topicWatcherProbe.expectMsg(15 second, KafkaTopicWatcherActor.GetTopics)
    topicWatcherProbe.reply(TopicsAvailable.empty)
    topicWatcherProbe.expectNoMsg(10 seconds)
    coordinatorProbe.expectMsg(TopicsAvailable.empty)
    //topicWatcherProbe.expectMsg(5 second, KafkaTopicWatcherActor.GetTopics)
    //topicWatcherProbe.expectMsg(5 second, KafkaTopicWatcherActor.GetTopics)

  }
  ignore should "supervise the topic watcher child" in {
    //val fuckedMaster = system.actorOf(MasterActor.props[ZKNotConnecting], name = "Master")
    //TestProbe()
    //    expectNoMsg(10 seconds)

  }

  ignore should "send to workers the topics discovered" in {
    val master = system.actorOf(MasterActor.props(topicWatcherMaker , coordinatorMaker))


  }

  ignore should "recover" in {}


}
/*
class ZKNotConnecting extends KafkaTopicWatcherActor {

  //override var client = null

  var numberOfTries = 0

  override def preStart(): Unit = {
    numberOfTries = numberOfTries + 1
    log.info(s"Number of tries: $numberOfTries")
    throw new RuntimeException("BUMM!!! Topic actor exploded when trying to connect")
  }

  override def receive = {
    case x => log.info(s"RECEIVING  MESSAGE: $x")
  }
}*/