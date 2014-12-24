package com.github.kuhnen.test.multinode

/**
 * Created by kuhnen on 12/18/14.
 */
//#package

//#config
import akka.remote.testkit.MultiNodeConfig
import akka.util.Timeout
import com.github.kuhnen.StartUp
import com.github.kuhnen.master.MasterActor
import com.github.kuhnen.master.kafka.KafkaTopicWatcherActor

import scala.concurrent.Await

object MultiNodeSampleConfig extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")
  //val node3 = role("node3")
}

//#spec
import akka.actor.{Actor, Props}
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender

class MultiNodeSampleSpecMultiJvmNode1 extends MultiNodeSample
class MultiNodeSampleSpecMultiJvmNode2 extends MultiNodeSample
//class MultiNodeSampleSpecMultiJvmNode3 extends MultiNodeSample

object MultiNodeSample {

//  class Ponger extends Actor {
//    def receive = {
//      case "ping" => sender() ! "pong"
 //   }
 // }
}

object MultiNodeSampleMaster {

}


class MultiNodeSample extends MultiNodeSpec(MultiNodeSampleConfig) with STMultiNodeSpec with ImplicitSender {

  import com.github.kuhnen.test.multinode.MultiNodeSample._
  import com.github.kuhnen.test.multinode.MultiNodeSampleConfig._
  import akka.pattern.ask
  import scala.concurrent.duration._

  def initialParticipants = roles.size
  println(s"Roles size: $initialParticipants")


  "A MultiNodeSample" must {

    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "send to and receive from a remote node" in {
      runOn(node1) {
        enterBarrier("deployed")
//        val ponger = system.actorSelection(node(node2) / "user" / "ponger")
  //      ponger ! "ping"
    //    expectMsg("pong")
      }

      runOn(node2) {
        //val master = system.actorOf(MasterActor.props[KafkaTopicWatcherActor])
        StartUp.startBackend(2551, "backend")
        implicit val timeout = Timeout(5 seconds)
        val worker = StartUp.startWorker(2555)
        val m = worker ask "Just fun"
        Await.ready(m, 5 seconds)


  //      system.actorOf(Props[Ponger], "ponger")
        enterBarrier("deployed")
      }

     // runOn(node3){
    //    enterBarrier("deployed")
     // }

      enterBarrier("finished")

    }
  }
}
//#spec

