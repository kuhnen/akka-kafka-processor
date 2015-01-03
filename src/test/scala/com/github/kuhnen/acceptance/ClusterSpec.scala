package com.github.kuhnen.acceptance

import akka.actor.ActorSystem
import com.github.kuhnen.CommonActorSpec
import com.github.kuhnen.cluster.{ClusterManager, ClusterConfig}
import com.typesafe.config.ConfigFactory

/**
 * Created by kuhnen on 1/3/15.
 */

class ClusterSpec(_system: ActorSystem) extends CommonActorSpec(_system) {

  //val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]").
  //q  withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
  //  withFallback(ConfigFactory.load())


  def this() = this(ActorSystem(ClusterConfig.clusterName))
  val cluster = new ClusterManager {
    override val system: ActorSystem = _system
  }

  val seedPort = 2551

  it should "work" in {

    cluster.startListener()
    Thread.sleep(1000)

    cluster.startMaster(seedPort)
    Thread.sleep(1000)

    cluster.startWorker()

    Thread.sleep(300000)


  }

}
