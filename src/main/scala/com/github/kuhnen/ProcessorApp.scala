package com.github.kuhnen

/**
 * Created by kuhnen on 12/16/14.
 */

import akka.actor._
import akka.contrib.pattern.{ClusterClient, ClusterSingletonManager}
import akka.japi.Util.immutableSeq
import com.github.kuhnen.cluster.ClusterConfig
import com.github.kuhnen.master.MasterActor.ActorBuilder
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor
import com.github.kuhnen.master.{WorkersCoordinator, MasterActor}
import com.typesafe.config.ConfigFactory

object StartUp {

  import ClusterConfig._

}

object ProcessorApp {
  //extends App {

  import com.github.kuhnen.StartUp._

  def main(args: Array[String]) {
 //    implicit val system = ActorSystem(clusterName)

   // val clusterListener = system.actorOf(Props[ClusterListener], name = "clusterListener")

  //  sys.addShutdownHook(system.shutdown())


    //    startupSharedJournal(system, startStore = (port == 2551), path =
    //     ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))

    //system.actorOf(ClusterSingletonManager.props(Master.props(workTimeout), "active",
    //  PoisonPill, Some(role)), "master")

    //system.actorOf(ClusterSingletonManager.props(Master.props, "active",
    // PoisonPill, Some(role)), "master")
  }

}
