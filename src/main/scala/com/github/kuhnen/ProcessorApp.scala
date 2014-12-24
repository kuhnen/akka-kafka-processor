package com.github.kuhnen

/**
 * Created by kuhnen on 12/16/14.
 */

import akka.actor._
import akka.contrib.pattern.{ClusterSingletonManager, ClusterClient}
import akka.japi.Util.immutableSeq
import com.github.kuhnen.ClusterConfig._
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor
import com.github.kuhnen.master.{MasterActor, Master, ClusterListener}
import com.github.kuhnen.worker.Worker
import com.typesafe.config.ConfigFactory


object StartUp {

  def startWorker(port: Int) = {
    // load worker.conf
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load("worker"))
    val system = ActorSystem("WorkerSystem", conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) =>
        println(addr)
        system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    //system.actorOf(Worker.props(clusterClient, Props[WorkExecutor]), "worker")
    system.actorOf(Worker.props(clusterClient), "worker")
  }

  def startBackend(port: Int, role: String): Unit = {
    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]").
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
      withFallback(ConfigFactory.load())
    val system = ActorSystem(clusterName, conf)
    system.actorOf(ClusterSingletonManager.props(MasterActor.props[KafkaTopicWatcherActor], "active", PoisonPill, Some(role)), "master")


  }
}
object ProcessorApp { //extends App {

  import StartUp._

  def main(args: Array[String]) {
  startBackend(2551, "backend")
  Thread.sleep(5000)
  // startBackend(2552, "backend")
  Thread.sleep(5000)
  startWorker(2555)
    implicit val system = ActorSystem(clusterName)

    val clusterListener = system.actorOf(Props[ClusterListener], name = "clusterListener")

    sys.addShutdownHook(system.shutdown())


    //    startupSharedJournal(system, startStore = (port == 2551), path =
    //     ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))

    //system.actorOf(ClusterSingletonManager.props(Master.props(workTimeout), "active",
    //  PoisonPill, Some(role)), "master")

    //system.actorOf(ClusterSingletonManager.props(Master.props, "active",
    // PoisonPill, Some(role)), "master")
  }

}
