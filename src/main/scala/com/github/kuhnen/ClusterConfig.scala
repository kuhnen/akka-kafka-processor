package com.github.kuhnen

/**
 * Created by kuhnen on 12/16/14.
 */

import com.typesafe.config.ConfigFactory

object ClusterConfig {

  private val config = ConfigFactory.load()
  val clusterName = config.getString("processor.cluster.name")

}