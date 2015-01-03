package com.github.kuhnen.worker.executor

import akka.actor.{Actor, ActorLogging}
import com.sclasen.akka.kafka.StreamFSM

/**
 * Created by kuhnen on 12/31/14.
 */
class PlainTextTopicActor extends Actor with ActorLogging {

  def receive = {

    case msg =>
      println(msg)
      sender() ! StreamFSM.Processed

  }

}




