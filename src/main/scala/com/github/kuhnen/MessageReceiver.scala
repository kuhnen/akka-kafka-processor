package com.github.kuhnen

import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date

import akka.actor.{ActorLogging, Props, ActorRef, Actor}
import com.sclasen.akka.kafka.StreamFSM
import org.joda.time.DateTime
import org.json4s.DefaultFormats
import org.json4s.ext.JodaTimeSerializers
import org.json4s.native.JsonMethods._

import scala.collection.immutable
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Created by kuhnen on 12/6/14.
 */

import org.json4s.native.Serialization._

case class SamplePojo(apikey: String, date: DateTime)

case class Message(topic: String, message: String)

class Reader[T](implicit ct: ClassTag[T] , mf: Manifest[T]) {

  val formatPattern = "yyyy-MM-dd HH:mm:ss"
  val formatHour = "yyyy-MM-dd'T'HH"

  implicit val formats = new DefaultFormats {
    override def dateFormatter: SimpleDateFormat = new SimpleDateFormat(formatPattern)
  } ++ JodaTimeSerializers.all

  def tryRead(msg: String): Try[T] =  {
    println("READING!!!!!!!!!!!!!!!!!!!!!!!")
    //printlln(msg)
    Thread.sleep(100)
    Try(read[T](msg))


  }

}

class TopicActor[T](topic: String, reader: Reader[T]) extends Actor with ActorLogging {

  val formatPattern = "yyyy-MM-dd HH:mm:ss"
  val formatHour = "yyyy-MM-dd'T'HH"
  //val writer = context.system.actorOf(Props(classOf[FileWriterActor], topic))
  var actorsByApikey = Map.empty[String, ActorRef]

  def newApiKeyActor(apiKey: String) = {
    log.info(s"Creating new actor for apiKey: $apiKey")
    //val pojo: SamplePojo.type = actorsByApikey.getOrElse(topic, SamplePojo)
    //val reader = new Reader[pojo.type]()
    val actor = context.system.actorOf(Props(classOf[FileWriterActor], topic, apiKey), name = apiKey) // TopicActor.props[pojo.type](topic, reader), name = topic)
    actorsByApikey = actorsByApikey.updated(apiKey, actor)
    actor

  }

  implicit val formats = new DefaultFormats {
    override def dateFormatter: SimpleDateFormat = new SimpleDateFormat(formatPattern)
  } ++ JodaTimeSerializers.all

  override def receive = {
    case msg: String =>
      //TODO parse pojo
      //val pojo: Try[T] = reader.tryRead(msg)
      //log.info(s"read $pojo")
      val msgJsonTR = Try (parse(msg))
      if (msgJsonTR.isFailure) {
        log.error(s"$msgJsonTR")
        sender ! StreamFSM.Processed
      }
      //TODO supervisor
      msgJsonTR.map { msgJson =>
        val date: DateTime = (msgJson \ "date").extractOrElse[DateTime](new DateTime("1900"))
        val apiKey = (msgJson \ "apiKey").extractOrElse[String]("no-apiKey")
        val hourFormat = date.toString(formatHour)
        val actor = actorsByApikey.getOrElse(apiKey, newApiKeyActor(apiKey))
        actor forward (MessageToWrite(hourFormat, msg))
      }
  }
}

case class MessageToWrite(hour: String,  event: String)

object TopicActor {
  def props[T](topic: String, reader: Reader[T]) = Props(classOf[TopicActor[T]], topic, reader)
}

import org.json4s.native.JsonMethods._

class FileWriterActor(topic: String, apiKey: String) extends Actor {

  import scala.concurrent.duration._
  val formatPattern = "yyyy-MM-dd HH:mm:ss"
  val formatHour = "yyyy-MM-dd'T'HH"

  implicit val formats = new DefaultFormats {
    override def dateFormatter: SimpleDateFormat = new SimpleDateFormat(formatPattern)
  } ++ JodaTimeSerializers.all

  var  timeMinuteBefore = DateTime.now.getMinuteOfDay
  // IDEA
  // kafka receiver -> transformation pipeline -> fileWriter actors
  def receive = {

    case MessageToWrite(hour, event) =>
      val file = new FileWriter("tmp/" + topic + "_" + hour+ "_" + apiKey + ".txt", true)
      file.append(event)
      file.append('\n')
      file.close()
      sender ! StreamFSM.Processed


//    case (topic,msg: String) =>
 //     val msgJsonTR =   Try { parse(msg) }
      //TODO supervisor
 //     msgJsonTR.map { msgJson =>
  //      val date: DateTime = (msgJson \ "date").extractOrElse[DateTime](new DateTime("1900"))
 //       val apiKey = (msgJson \ "apiKey").extractOrElse[String]("no-apiKey")
 //       val hourFormat = date.toString(formatHour)
  //      val file = new FileWriter("tmp/" + topic + "_" + hourFormat + "_" + apiKey + ".txt", true)
  //      file.append(msg)
   //     file.append('\n')
   //     file.close()
    //  }
      // IF time is closed, ask some other actor to compact the file after compacting the file
      //some other actor should send to s3
     // sender ! StreamFSM.Processed
  }
}

class MessageReceiver extends Actor with ActorLogging {

  var topicActor: Map[String, ActorRef] = Map.empty[String, ActorRef]
  val topicToPojo: immutable.Map[String, SamplePojo.type] = Map("views" -> SamplePojo)

  def newTopicActor(topic: String) = {
    log.info(s"Creating new actor for topic: $topic")
    val pojo: SamplePojo.type = topicToPojo.getOrElse(topic, SamplePojo)
    val reader = new Reader[pojo.type]()
    val actor = context.system.actorOf(TopicActor.props[pojo.type](topic, reader), name = topic)
    topicActor = topicActor.updated(topic, actor)
    actor

  }

  override def receive= {

    case Message(topic, message) =>
      log.debug(s"Got topic $topic, and message: $message")
      val actor = topicActor.getOrElse(topic,newTopicActor(topic))
      actor forward message
    case x =>
      println("THIS SHOULD NOT HAPPEN")
      //println(x)
  }

}
