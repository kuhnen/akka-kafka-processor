package com.github.kuhnen.worker.executor

import com.sclasen.akka.kafka.AkkaConsumerProps
import kafka.serializer.StringDecoder

/**
 * Created by kuhnen on 12/31/14.
 */
class AkkaKafkaConsumer {

  //AkkaConsumerProps.forContext()
/*
  AkkaConsumerProps.forSystem(
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
*/
}
