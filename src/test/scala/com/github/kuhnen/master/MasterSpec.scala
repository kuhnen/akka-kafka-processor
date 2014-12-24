package com.github.kuhnen.master

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import com.github.kuhnen.CommonActorSpec
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor
import org.scalatest.DoNotDiscover

import scala.concurrent.duration._

/**
 * Created by kuhnen on 12/20/14.
 */

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

@DoNotDiscover
class MasterSpec(_system: ActorSystem) extends CommonActorSpec(_system) {

  def this() = this(ActorSystem("test"))

  //implicit val _system = system



  ignore should "supervise the topic watcher child" in {
    //val fuckedMaster = system.actorOf(MasterActor.props[ZKNotConnecting], name = "Master")
    //TestProbe()
    //    expectNoMsg(10 seconds)

  }

  it should "send to workers the topics discovered" in {
    val master = system.actorOf(MasterActor.props[KafkaTopicWatcherActor])
    val worker1 = TestProbe()
    val worker2 = TestProbe()
    //master ! RegisterWorker(worker1.ref)
    //master ! RegisterWorker(worker2.ref)

    worker1.expectMsgPF(15 seconds) {
      case msg => println(msg)
    }

  }

  ignore should "recover" in {}


}