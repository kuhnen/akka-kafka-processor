package com.github.kuhnen.master

import akka.actor.{Props, Actor, ActorLogging}
import com.github.kuhnen.master.KafkaTopicsWatcher.Topics
import kafka.utils.ZKStringSerializer
import org.I0Itec.zkclient.ZkClient

/**
 * Created by kuhnen on 12/16/14.
 */
class KafkaTopicsWatcher extends Actor with ActorLogging {

  var zkClient: ZkClient = _
  var topicList: Seq[String] = Nil
  //  val zkClient = new ZkClient(zkConnect, 30000, 30000, ZKStringSerializer)
  //  //if (topic == "")
  //  topicList = ZkUtils.getChildrenParentMayNotExist(zkClient, ZkUtils.BrokerTopicsPath).sorted
  //  topicList foreach println
  //sys.exitimport akka.actor.{ActorSystem, Props}


  //Open connection to zookeepr
 override def preStart() {

  //  ConfigFacgory
   var zkConnect: String = ""
   //zkClient = new ZkClient(zkConnect, 30000, 30000, ZKStringSerializer)

  }


  //CLoseConnection
  override def postStop() = {
    zkClient.close()

  }

  override def receive = {

    case Topics =>

  }

}


object KafkaTopicsWatcher {

  trait KafkaTopicsWatcherMessage

  object Topics extends KafkaTopicsWatcherMessage

  def props() = Props[KafkaTopicsWatcher]
}
