package com.github.kuhnen

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

/**
 * Created by kuhnen on 12/20/14.
 */
trait CommonSpecs extends FlatSpecLike with Matchers with BeforeAndAfterAll with ScalaFutures

//
//
// trait ActorCommonSpecs extends ImplicitSender {

// val system = ActorSystem("test")

//}

abstract class CommonActorSpec(system: ActorSystem) extends TestKit(system) with ImplicitSender with CommonSpecs {

  //val _system: ActorSystem = s

  override def beforeAll() = {}

  override def afterAll() = TestKit.shutdownActorSystem(system)


}

object LocalConf {

  val confStr = """akka {
    actor.provider = "akka.actor.LocalActorRefProvider"
    loggers = [akka.event.slf4j.Slf4jLogger]
    loglevel = INFO
  }
    processor.cluster.master.path = "/user/master/active"
                """
  val conf = ConfigFactory.parseString(confStr)


}


