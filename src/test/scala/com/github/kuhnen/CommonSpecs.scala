package com.github.kuhnen

import akka.actor.ActorSystem
import akka.testkit.{TestKit, ImplicitSender}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpecLike, WordSpecLike, Matchers, BeforeAndAfterAll}

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

  override def beforeAll() = { }

  override def afterAll() = TestKit.shutdownActorSystem(system)


}


