package com.github.kuhnen.cluster

/**
 * Created by kuhnen on 12/16/14.
 */

import com.typesafe.config.ConfigFactory

object ClusterConfig {

  val config = ConfigFactory.load()
  val clusterName = config.getString("processor.cluster.name")

}