package com.github.kuhnen.worker

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import akka.contrib.pattern.ClusterClient.SendToAll

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
  import Worker._
  import scala.concurrent.duration._
  implicit val ec =  context.system.dispatcher

  context.system.scheduler.schedule(5 seconds, 5 seconds) {
    println("Sending message")
    sendToMaster("UI UI UI")
  }

  def receive = {
    case x => log.error(s"x")
  }
  def sendToMaster(msg: Any): Unit = {
    println(singletonMasterPath)
    clusterClient ! SendToAll(singletonMasterPath, msg)
  }

}
