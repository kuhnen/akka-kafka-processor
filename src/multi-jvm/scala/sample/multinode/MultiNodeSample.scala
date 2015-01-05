//#package
package sample.multinode
//#package

//#config

import akka.actor.{ActorPath, PoisonPill, ActorSystem}
import akka.remote.testkit.MultiNodeConfig
import com.github.kuhnen.cluster.{ClusterConfig, ClusterManager}
import com.typesafe.config.ConfigFactory


object MultiNodeSampleConfig extends MultiNodeConfig {
  import ConfigFactory._
  val node1 = role("node1")
  val node2 = role("node2")
  val node3 = role("node3")

  val clusterProvider = parseString(s"""akka.actor.provider = "akka.cluster.ClusterActorRefProvider"""")
  nodeConfig(node1)(conf(6666)
    .withFallback(confWithRoles("master")
    .withFallback(ConfigFactory.load())))
  nodeConfig(node2)(conf(7777)
    .withFallback(confWithRoles("master")
    .withFallback(ConfigFactory.load())))

  nodeConfig(node3)(conf(8888)
    .withFallback(confWithRoles("master"))
    .withFallback(ConfigFactory.load()))

  def conf(port: Int) = clusterProvider
    .withFallback(debugConfig(false))
    .withFallback(receptionist)
    .withFallback(confWithSeedOnPort(6666)
    .withFallback(confWithNettyPort(port)))



  def receptionist = parseString("""extensions = ["akka.contrib.pattern.ClusterReceptionistExtension"]""")

  def confWithNettyPort(port: Int) =  ConfigFactory.parseString(s"akka.remote.netty.tcp.port = $port")

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
import akka.actor.{ Props, Actor }

class MultiNodeSampleSpecMultiJvmNode1 extends MultiNodeSample
class MultiNodeSampleSpecMultiJvmNode2 extends MultiNodeSample
class MultiNodeSampleSpecMultiJvmNode3 extends MultiNodeSample

class TestClusterManager(portNetty:  Int, seedPort: Int,
                         roles: List[String] = List.empty,
                         _system: Option[ActorSystem] = None) extends ClusterManager {
  import MultiNodeSampleConfig._

  val rolesAsStr = "[" +roles.mkString(",") + "]"
  val rolesConf = ConfigFactory.parseString(s"akka.cluster.roles=$rolesAsStr")
  val conf = rolesConf.withFallback(confWithNettyPort(portNetty))
    .withFallback(confWithSeedOnPort(seedPort))
    .withFallback(ConfigFactory.load())

  val thisSystem =  _system.getOrElse(ActorSystem(ClusterConfig.clusterName, conf))
  override val system: ActorSystem = thisSystem

}

class MultiNodeSample extends MultiNodeSpec(MultiNodeSampleConfig)
with STMultiNodeSpec with ImplicitSender {



  import MultiNodeSampleConfig._

  val fiveSeconds = 5000
  def initialParticipants = 1 //roles.size

  "A MultiNodeSample" must {
    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "start nodes" in {
      runOn(node1) {
        enterBarrier("deployed")
        val _system = system
 //       val manager = new TestClusterManager(6666, 6666)
 val _s = system
 val manager = new ClusterManager {  override val system: ActorSystem = _s  }//TestClusterManager(2555, 6666, List("master"), Option(system))
        manager.startListener()
        //Thread.sleep(fiveSeconds )
        manager.startMaster()
       // val node3Ref = system.actorSelection(node(node3))
        //startNewSystem()
        //println(s"KILLINGGG!!!!!!!!!!!!!!!!!!!!!! $node3Ref")
     //   node3Ref ! PoisonPill

  //      manager.startMaster()
        //manager.startMaster()

      }

      runOn(node2) {
        //Thread.sleep(fiveSeconds + 5000)
        enterBarrier("deployed")
        val _s = system
        val manager = new ClusterManager {  override val system: ActorSystem = _s  }//TestClusterManager(2555, 6666, List("master"), Option(system))
      //  val manager = new TestClusterManager(9999, 6666, List("master"))
         manager.startMaster()
        //nodeActor ! PoisonPill
      }

      runOn(node3) {
        Thread.sleep(fiveSeconds - 2000)

        val _s = system
        val manager = new ClusterManager {  override val system: ActorSystem = _s  }//TestClusterManager(2555, 6666, List("master"), Option(system))
        manager.startMaster()
        enterBarrier("deployed")

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

     Thread.sleep(26000)
     enterBarrier("finished")
    }
  }
}
//#spec









