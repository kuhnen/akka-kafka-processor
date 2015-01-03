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

abstract class CommonActorSpec(system: ActorSystem) extends TestKit(system) with ImplicitSender with CommonSpecs {

  override def beforeAll() = {}

  override def afterAll() = TestKit.shutdownActorSystem(system)

}

object LocalConf {

  val confStr = """akka {
    actor.provider = "akka.actor.LocalActorRefProvider"
    #loggers = [akka.event.slf4j.Slf4jLogger, "akka.testkit.TestEventListener"]
    loggers = ["akka.testkit.TestEventListener"]
    loglevel = DEBUG

                   actor {
                      debug {
                        # enable function of Actor.loggable(), which is to log any received message
                        # at DEBUG level, see the “Testing Actor Systems” section of the Akka
                        # Documentation at http://akka.io/docs
                        receive = on

                        # enable DEBUG logging of all AutoReceiveMessages (Kill, PoisonPill et.c.)
                        autoreceive = on

                        # enable DEBUG logging of actor lifecycle changes
                        lifecycle = on

                        # enable DEBUG logging of all LoggingFSMs for events, transitions and timers
                        fsm = on

                        # enable DEBUG logging of subscription changes on the eventStream
                        event-stream = on

                        # enable DEBUG logging of unhandled messages
                        unhandled = on

                        # enable WARN logging of misconfigured routers
                        router-misconfiguration = on
                      }
                    }
  }
    processor.cluster.master.path = "/user/master/active"
                """
  val conf = ConfigFactory.parseString(confStr)


}

