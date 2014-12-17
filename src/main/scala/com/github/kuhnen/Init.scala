/*package com.github.kuhnen

import akka.util.Timeout
import com.sclasen.akka.kafka.CommitConfig
import kafka.consumer.Whitelist
import kafka.message.MessageAndMetadata
import kafka.serializer.StringDecoder
import org.I0Itec.zkclient.ZkClient

import scala.concurrent.duration.FiniteDuration

/**
 * Created by kuhnen on 12/6/14.
 */
object Init extends App {

//  val zkConnect = "localhost"
 // var topicList: Seq[String] = Nil
  //val zkClient = new ZkClient(zkConnect, 30000, 30000, ZKStringSerializer)
  //if (topic == "")
   // topicList = ZkUtils.getChildrenParentMayNotExist(zkClient, ZkUtils.BrokerTopicsPath).sorted

  import akka.actor.{Props, ActorSystem, Actor}
  import com.sclasen.akka.kafka.{AkkaConsumer, AkkaConsumerProps, StreamFSM}
  import kafka.serializer.DefaultDecoder

    val system = ActorSystem("test")
    implicit val executionContext = system.dispatcher
    //val fileWrite = system.actorOf(Props[FileWriterActor])
    val msgReceiver = system.actorOf(Props[MessageReceiver])

    val messageH: (MessageAndMetadata[String,String]) => Message = {
      msg =>
       val event =  msg.message()
        val off = msg.offset
        val topic = msg.topic
        Message(topic , event)

    }

  import scala.concurrent.duration._
    var consumerProps = AkkaConsumerProps.forSystemWithFilter(
      system = system,
      zkConnect = "localhost:2181", //"platform-kafka-02.chaordicsystems.com:2181",
      topicFilter = new Whitelist(".*"),
      group = "dump",
      streams = 4, //one per partition
      keyDecoder = new StringDecoder(),
      msgDecoder = new StringDecoder(),
      msgHandler = messageH,
      receiver = msgReceiver,
      maxInFlightPerStream = 64,
      startTimeout = 100 seconds

    )

   consumerProps = AkkaConsumerProps.forSystem(
    system = system,
    zkConnect = "platform-kafka-02.chaordicsystems.com:2181",// "localhost:2181", //"platform-kafka-02.chaordicsystems.com:2181",
    topic = "view",
    group = "dump",
    streams = 1, //one per partition
    keyDecoder = new StringDecoder(),
    msgDecoder = new StringDecoder(),
    msgHandler = messageH,
    receiver = msgReceiver,
    maxInFlightPerStream = 1,
    startTimeout = 100 seconds

  )



  val consumer = new AkkaConsumer(consumerProps)

    val s = consumer.start()  //returns a Future[Unit] that completes when the connector is started

    s.onComplete{x =>
      println("started")
      println(x) }

    val commited = consumer.commit() //returns a Future[Unit] that completes when all in-flight messages are processed and offsets are committed.

 // commited.onComplete{x =>
 //   println("commited")
 //   println(x)
 // }

 //val stoped =    consumer.stop()   //returns a Future[Unit] that completes when the connector is stopped.

//stoped.onComplete {x=>
 // println("Stoped")
 // println(x)
//}




}
*/