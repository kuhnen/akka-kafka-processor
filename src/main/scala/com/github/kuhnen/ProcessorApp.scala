package com.github.kuhnen

/**
 * Created by kuhnen on 12/16/14.
 */

import akka.actor._
import com.github.kuhnen.master.ClusterListener
import com.github.kuhnen.master.worker.Master
import com.github.kuhnen.worker.Worker
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.RootActorPath
import akka.cluster.Cluster
import akka.contrib.pattern.ClusterClient
import akka.contrib.pattern.ClusterSingletonManager
import akka.japi.Util.immutableSeq
import akka.actor.AddressFromURIString
import akka.actor.ActorPath
import akka.persistence.journal.leveldb.SharedLeveldbStore
import akka.util.Timeout
import akka.pattern.ask
import akka.actor.Identify
import akka.actor.ActorIdentity
import akka.persistence.journal.leveldb.SharedLeveldbJournal



object ProcessorApp extends App {

  import com.github.kuhnen.ClusterConfig._

  startBackend(2551, "backend")
  Thread.sleep(5000)
 // startBackend(2552, "backend")
  Thread.sleep(5000)
  startWorker(2555)



  def startBackend(port: Int, role: String): Unit = {
    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]").
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
      withFallback(ConfigFactory.load())
    val system = ActorSystem(clusterName, conf)

//    startupSharedJournal(system, startStore = (port == 2551), path =
 //     ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))

    //system.actorOf(ClusterSingletonManager.props(Master.props(workTimeout), "active",
    //  PoisonPill, Some(role)), "master")

    system.actorOf(ClusterSingletonManager.props(Master.props, "active",
      PoisonPill, Some(role)), "master")
  }

  def startWorker(port: Int): Unit = {
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
    system.actorOf(Worker.props(clusterClient),  "worker")
  }


  implicit val system = ActorSystem(clusterName)

  val clusterListener = system.actorOf(Props[ClusterListener], name = "clusterListener")

  sys.addShutdownHook(system.shutdown())
}
