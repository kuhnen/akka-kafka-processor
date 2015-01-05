package com.github.kuhnen.cluster

/**
 * Created by kuhnen on 12/16/14.
 */

import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

object ClusterConfig {

  val config = ConfigFactory.load()
  val clusterName = config.getString("processor.cluster.name")
  val watcherInitialDelay = config.getInt("kafka.topic.watcher.initialDelay") seconds
  val watcherInterval = config.getInt("kafka.topic.watcher.interval") seconds

}