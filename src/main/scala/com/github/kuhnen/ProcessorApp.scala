package com.github.kuhnen

/**
 * Created by kuhnen on 12/16/14.
 */

import akka.actor._
import com.github.kuhnen.master.ClusterListener

object ProcessorApp extends App {

  import com.github.kuhnen.ClusterConfig._

  implicit val system = ActorSystem(clusterName)

  val clusterListener = system.actorOf(Props[ClusterListener], name = "clusterListener")

  sys.addShutdownHook(system.shutdown())
}
