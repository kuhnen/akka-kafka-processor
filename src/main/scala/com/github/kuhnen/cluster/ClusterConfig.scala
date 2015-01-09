package com.github.kuhnen.cluster

/**
 * Created by kuhnen on 12/16/14.
 */

import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

object ClusterConfig {
  //Name alligator
  import ConfigFactory._

  val config = load()
  //val akkaConf = load("akka")
  //val kafkaConf = load("kafka")
  val clusterName = config.getString("processor.cluster.name")
  val watcherInitialDelay = config.getInt("kafka.topic.watcher.initialDelay") seconds
  val watcherInterval = config.getInt("kafka.topic.watcher.interval") seconds
  //val roles = akkaConf.getStringList("cluster.roles")

}