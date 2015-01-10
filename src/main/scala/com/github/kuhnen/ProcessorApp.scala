package com.github.kuhnen

/**
 * Created by kuhnen on 12/16/14.
 */

import akka.actor._
import com.github.kuhnen.cluster.ClusterManager
import com.typesafe.config.ConfigFactory
import org.json4s.DefaultFormats

object ProcessorApp {

  import com.github.kuhnen.cluster.ClusterConfig._

  def main(args: Array[String]) {

    //args foreach println

    println(alligatorConf)

    Thread.sleep(5000)
    implicit val formats = DefaultFormats
    val roles = sys.env.get("ROLES")

    val conf = {
      if (roles.isDefined)
        ConfigFactory.parseString(s"akka.cluster.roles=$roles").withFallback(config)
      else
        config
    }

    implicit val _system = ActorSystem(clusterName, conf)

    val clusterManager = new ClusterManager {
      override val system: ActorSystem = _system
    }

    import clusterManager._
    startListener
    startMaster()
    startWorker
    sys.addShutdownHook(_system.shutdown())


    //    startupSharedJournal(system, startStore = (port == 2551), path =
    //     ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))

    //system.actorOf(ClusterSingletonManager.props(Master.props(workTimeout), "active",
    //  PoisonPill, Some(role)), "master")

    //system.actorOf(ClusterSingletonManager.props(Master.props, "active",
    // PoisonPill, Some(role)), "master")
  }

}
