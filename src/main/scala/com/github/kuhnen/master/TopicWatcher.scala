package com.github.kuhnen.master

/**
 * Created by kuhnen on 12/20/14.
 */


object TopicWatcher {

  case class Topics(topics :Set[String])

  object Topics {
    def empty = Topics(Set.empty)
  }
  //type Topics = Set[String]

}

trait TopicWatcher[T] {



  var client: T

  def topics(): Set[String]

}
