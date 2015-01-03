package com.github.kuhnen.util.kafka

import com.sclasen.akka.kafka.AkkaConsumer
import kafka.producer.{KeyedMessage, ProducerConfig, Producer}

/**
 * Created by kuhnen on 1/3/15.
 */

object KafkaUtil {

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
