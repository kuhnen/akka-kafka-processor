package com.github.kuhnen.worker

import akka.actor._
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.testkit.TestProbe
import com.github.kuhnen.master.MasterWorkerProtocol.RegisterWorkerOnCluster
import com.github.kuhnen.master.WorkersCoordinator.Work
import com.github.kuhnen.{CommonActorSpec, CommonSpecs, LocalConf}
import com.sclasen.akka.kafka.{StreamFSM, AkkaConsumer}
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}

import scala.concurrent.duration._

/**
 * Created by kuhnen on 12/27/14.
 */


object KafkaUtilSpec {

  type Key = Array[Byte]
  type Msg = Array[Byte]
  type MsgProducer = Producer[Key, Msg]
  val messages = 1000

  def kafkaProducer = new MsgProducer(new ProducerConfig(kafkaProducerProps))
  lazy val producer = kafkaProducer

  def kafkaProducerProps = AkkaConsumer.toProps(collection.mutable.Set(
    "metadata.broker.list" -> "localhost:9092",
    "producer.type" -> "sync",
    "request.required.acks" -> "-1")
  )

  def sendMessages(topic: String) {
    (1 to messages).foreach {
      num =>
        producer.send(new KeyedMessage(topic, num.toString.getBytes, num.toString.getBytes))
    }
  }


}

class WorkerSpec(_system: ActorSystem) extends CommonActorSpec(_system) {

  import KafkaUtilSpec._
  def this() = this(ActorSystem("WorkerSpec", LocalConf.conf))


  val probe = TestProbe()
  val clusterClientProbe = TestProbe()

  def childMaker = (_: ActorRefFactory, topic: String) => probe.ref

  def createWorker() = system.actorOf(KafkaWorker.props(clusterClientProbe.ref, childMaker))

  it should "register it self on the master node with the right path" in {

    val clusterClientProbe = TestProbe()
    //val worker = system.actorOf(Worker.props(clusterClientProbe.ref, Props[DummyKafkaWorker]))
    val worker = system.actorOf(KafkaWorker.props(clusterClientProbe.ref, childMaker))
    clusterClientProbe.expectMsgPF() {
      case SendToAll(KafkaWorker.singletonMasterPath, RegisterWorkerOnCluster(_)) => assert(true)
    }
  }

  it should "should start a consumer for some topic" in {
    val worker = system.actorOf(KafkaWorker.props(clusterClientProbe.ref, childMaker))
    worker ! Work("sometopic")
    expectMsg(10 seconds, ())

  }

  it should "begin to send messages from kafka to the executor" in {
    val worker = system.actorOf(KafkaWorker.props(clusterClientProbe.ref, childMaker))
    val topic = "my-topic"
    sendMessages(topic)
    worker ! Work(topic)
    expectMsg(())
    for (_ <- 1 to messages){
      probe.expectMsgPF(5 seconds) {
        case _ => assert(true)
      }
      probe.reply(StreamFSM.Processed)
    }
  }

}

class ConsumerConfigSpec extends CommonSpecs {


  import com.github.kuhnen.worker.ConsumerConfig._

  it should "get the topic commit interval configuration or None" in {
    getTopicCommitInterval("view") shouldBe Some(10 seconds)

  }

  it should "get the default value if topic no existent" in {
    getTopicCommitInterval("bla") shouldBe Some(2 seconds)

  }

  it should "get the None if default value no existent" in {
    getTopicCommitAfterMessageCount("bla") shouldBe None

  }

  it should "get the topic msg count commit" in {
    getTopicCommitAfterMessageCount("view") shouldBe Some(100)

  }

  it should "get the topic maxInFlight" in {
    getTopicMaxInFlightPerStream("view") shouldBe 1

  }

}