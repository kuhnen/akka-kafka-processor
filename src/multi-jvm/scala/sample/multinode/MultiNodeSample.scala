//#package
package sample.multinode

//#package

//#config

import akka.actor.{Address, ActorPath, PoisonPill, ActorSystem}
import akka.cluster.Cluster
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import com.github.kuhnen.cluster.{ClusterConfig, ClusterManager}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.duration._


object MultiNodeSampleConfig extends MultiNodeConfig {

  import com.typesafe.config.ConfigFactory._

  val node1 = role("node1")
  val node2 = role("node2")
  val node3 = role("node3")

  val clusterProvider = parseString( s"""akka.actor.provider = "akka.cluster.ClusterActorRefProvider"""")

  nodeConfig(node1)(conf(6666)
    .withFallback(noReceptionist)
    //.withFallback(confWithRoles("master"))
    .withFallback(ConfigFactory.load()))

  nodeConfig(node2)(conf(7777)
    .withFallback(confWithRoles("master")
    .withFallback(ConfigFactory.load())))

  nodeConfig(node3)(conf(8888)
    .withFallback(confWithRoles("master"))
    .withFallback(ConfigFactory.load()))

  def conf(port: Int) = clusterProvider
    .withFallback(debugConfig(false))
    .withFallback(confWithSeedOnPort(6666)
    .withFallback(confWithNettyPort(port)))


  def noReceptionist = parseString( """extensions = []""")

  def confWithNettyPort(port: Int) = ConfigFactory.parseString(s"akka.remote.netty.tcp.port = $port")

  def confWithNettyPortAndSeed(port: Int) = {
    confWithNettyPort(port).withFallback(confWithSeedOnPort(port))
  }

  def confWithSeedOnPort(port: Int) = {
    ConfigFactory.parseString(
      s"""akka.cluster.seed-nodes = ["akka.tcp://"${ClusterConfig.clusterName}"@127.0.0.1:$port"]""".stripMargin
    )
  }

  def confWithRoles(roles: String*) = {
    val rolesAsStr = "[" + roles.mkString(",") + "]"
    ConfigFactory.parseString(s"akka.cluster.roles=$rolesAsStr")
  }

}

//#config

//#spec

import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender

class MultiNodeSampleSpecMultiJvmNode1 extends MultiNodeSample

class MultiNodeSampleSpecMultiJvmNode2 extends MultiNodeSample

class MultiNodeSampleSpecMultiJvmNode3 extends MultiNodeSample

class TestClusterManager(portNetty: Int, seedPort: Int,
                         roles: List[String] = List.empty,
                         _system: Option[ActorSystem] = None) extends ClusterManager {

  import sample.multinode.MultiNodeSampleConfig._

  val rolesAsStr = "[" + roles.mkString(",") + "]"
  val rolesConf = ConfigFactory.parseString(s"akka.cluster.roles=$rolesAsStr")
  val conf = rolesConf.withFallback(confWithNettyPort(portNetty))
    .withFallback(confWithSeedOnPort(seedPort))
    .withFallback(ConfigFactory.load())

  val thisSystem = _system.getOrElse(ActorSystem(ClusterConfig.clusterName, conf))
  override val system: ActorSystem = thisSystem

}

class MultiNodeSample extends MultiNodeSpec(MultiNodeSampleConfig)
with STMultiNodeSpec with ImplicitSender with LazyLogging {


  import sample.multinode.MultiNodeSampleConfig._

  val fiveSeconds = 5000

  def initialParticipants = roles.size

  val cluster = Cluster(system)
  class TestClusterManager(implicit override val system: ActorSystem) extends ClusterManager

  implicit def roleNameToAddress(role: RoleName): Address = testConductor.getAddressFor(role).await
  implicit var sys: ActorSystem = system

  val managerT = new TestClusterManager

  runOn(node1) {

    log.info(s"Starting node $node1")
    //val manager = new TestClusterManager()
    managerT.startListener()
    enterBarrier("deployed")
  }

  Thread.sleep(5000)
  runOn(node1) { cluster join node1}
  runOn(node2) { cluster join node1}
  Thread.sleep(5000)
  runOn(node3) { cluster join node1}
  Thread.sleep(5000)

  runOn(node2) {

    log.info(s"Starting master and worker on node $node2")
    //val manager = new TestClusterManager
    managerT.startMaster()
    managerT.startWorker()
    enterBarrier("deployed")
  }

  Thread.sleep(5000)

  runOn(node3) {
    enterBarrier("deployed")
    log.info(s"Starting master and worker on node $node3")
    //val manager = new TestClusterManager()
    managerT.startMaster()
    managerT.startWorker()
  }



  //This is done just to show that the oldest node on the cluster take over the master
  // not the oldest master
  //TODO REGISTER SHOULD CONFIRM

  enterBarrier("Wait")
  Thread.sleep(5000)
  log.info("Waiting some more time so node 2 can leave")
  Thread.sleep(5000)

  runOn(node2) {
    cluster.leave(node2)
    Thread.sleep(5000)
//    cluster.down(node2)
    Thread.sleep(5000)
    system.shutdown()
    system.awaitTermination(10 seconds)

  }

  Thread.sleep(15000)

  runOn(node2) {
    sys = startNewSystem()

    cluster.join(node1)
    val manager = new ClusterManager{
      override val system: ActorSystem = sys
    }
    manager.startMaster()
    manager.startWorker()
  }

  Thread.sleep(15000)
  //Let's send some information to kafka
  runOn(node1) {
    log.info("Sending topics to kafka")
    //ONE TOPIC
    //Another TOPIC
   //And 2 more topics
  }





  //Now let's shutdown on node and see if the working node takes the topic


  //TODO
  //Now let's put back the node down on cluster and it should take over one  topic

  //runOn(node1) { cluster.leave(node2)}
  //runOn(node2) { system.shutdown()}
  //Thread.sleep(10000)
  //IT DOES NOT JOIN AGAIN, NEEDS TO RESTART THE SYSTEM Docs says that
  //runOn(node2) { cluster.join(node1)}
  //runOn(node2) { startNewSystem()}
  //Thread.sleep(5000)
  //runOn(node2) { cluster join node1 }
  Thread.sleep(45000)

  /*"A MultiNodeSample" must {
    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "start nodes" in {
      runOn(node1) {

        val _system = system
        //       val manager = new TestClusterManager(6666, 6666)
        val _s = system
        val manager = new ClusterManager {
          override val system: ActorSystem = _s
        }
        // val thisManager =  new TestClusterManager(2555, 6666, List("master"))
        manager.startListener()
        enterBarrier("deployed")
        Thread.sleep(fiveSeconds + 25000)
        //manager.startMaster()
        //thisManager.startMaster()

        val a: ActorPath = node(node2)

         val node2Ref = system.actorSelection(node(node2))
        //startNewSystem()
        println(s"KILLINGGG!!!!!!!!!!!!!!!!!!!!!! $node2Ref")
        node2Ref ! PoisonPill
        Thread.sleep(20000)
       // enterBarrier("kill")
       // enterBarrier("finished")
       //
        //   node3Ref ! PoisonPill

        //      manager.startMaster()
        //manager.startMaster()

      }

      runOn(node2) {
        enterBarrier("deployed")
        val _s = system
        val manager = new ClusterManager {
          override val system: ActorSystem = _s
        } //TestClusterManager(2555, 6666, List("master"), Option(system))
        val manager2 = new TestClusterManager(9999, 6666, List("master"))
        manager.startMaster()
        manager2.startMaster()
        Thread.sleep(20000)
        system.shutdown()
        //manager2.thisSystem.shutdown()


       // enterBarrier("kill")
       // enterBarrier("finished")

        //nodeActor ! PoisonPill
      }


      runOn(node3) {
        enterBarrier("deployed")
        val _s = system
        val manager = new ClusterManager {
          override val system: ActorSystem = _s
        } //TestClusterManager(2555, 6666, List("master"), Option(system))
        manager.startMaster()

//        enterBarrier("kill")
//        enterBarrier("Finished")

       //nodeActor ! PoisonPill
      }



      // val path1 = node(node1)
      // val path2= node(node2)
      //val path3= node(node3)
      //println(s"Nodes: ${path1 :: path2 :: path3 :: Nil}")
      // runOn(node1) {
      //  enterBarrier("launchMaster")
      // Thread.sleep(5000)
      //println("Shutting down node")
      //val nodeActor = system.actorSelection(node(node2))

      //}

      Thread.sleep(90000)
      //enterBarrier("finished")
    }
  }*/
}

//#spec









