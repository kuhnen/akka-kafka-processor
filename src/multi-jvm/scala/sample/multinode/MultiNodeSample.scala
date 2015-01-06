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
with STMultiNodeSpec with ImplicitSender {


  import sample.multinode.MultiNodeSampleConfig._

  val fiveSeconds = 5000

  def initialParticipants = roles.size

  val cluster = Cluster(system)

  implicit def roleNameToAddress(role: RoleName): Address = testConductor.getAddressFor(role).await
  implicit val sys: ActorSystem = system

  runOn(node1) {
    val manager = new ClusterManager {
      override val system: ActorSystem = sys
    }
    manager.startListener()
 //   manager.startMaster()
//    manager.startWorker()
  }

  Thread.sleep(5000)
  runOn(node1) { cluster join node1}
  runOn(node2) { cluster join node1}
  Thread.sleep(5000)
  runOn(node3) { cluster join node1}
  Thread.sleep(5000)
  runOn(node3) {
    val manager = new ClusterManager {
      override val system: ActorSystem = sys
    }
    manager.startMaster()
    Thread.sleep(5000)
    manager.startWorker()
  }

  Thread.sleep(5000)
  runOn(node2) {
    val manager = new ClusterManager {
      override val system: ActorSystem = sys
    }
    manager.startMaster()
    Thread.sleep(5000)
    manager.startWorker()
  }

  Thread.sleep(15000)
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









