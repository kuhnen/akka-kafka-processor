package com.github.kuhnen.test.multinode


import akka.remote.testkit.MultiNodeSpecCallbacks
import org.scalatest.{WordSpecLike, Matchers, BeforeAndAfterAll}

/**
 * Hooks up MultiNodeSpec with ScalaTest
 */
trait STMultiNodeSpec extends MultiNodeSpecCallbacks with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}

