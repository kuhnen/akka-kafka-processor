package com.github.kuhnen.worker

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.contrib.pattern.ClusterClient.SendToAll
import com.github.kuhnen.master.MasterWorkerProtocol.RegisterWorkerOnCluster

/**
 * Created by kuhnen on 12/17/14.
 */


object Worker {

  val singletonMasterPath = "/user/master/active"

  def props(clusterClient: ActorRef): Props = Props(classOf[Worker], clusterClient)

  //def props(clusterClient: ActorRef, workExecutorProps: Props, registerInterval: FiniteDuration = 10.seconds): Props =
  //Props(classOf[Worker], clusterClient, workExecutorProps, registerInterval)

  //case class WorkComplete(result: Any)
}

class Worker(clusterClient: ActorRef) extends Actor with ActorLogging {

  import com.github.kuhnen.worker.Worker._

  implicit val ec = context.system.dispatcher

  // context.system.scheduler.schedule(5 seconds, 5 seconds) {
  //   println("Sending message")
  //   sendToMaster("UI UI UI")
  // }
  override def preStart(): Unit = {
    sendToMaster(RegisterWorkerOnCluster(self))
  }

  def receive = {
    case x =>
      log.info(s"Just testing working getting a message $x")
      sender ! x
  }

  def sendToMaster(msg: Any): Unit = {
    println(singletonMasterPath)
    clusterClient ! SendToAll(singletonMasterPath, msg)
  }

}
