package com.caxton.fitman.mongodb

import java.math.BigInteger
import java.security.SecureRandom

import org.joda.time.Instant
import org.mongodb.scala._
import org.scalatest.{FlatSpec, Matchers}
import scala.util.Random

/**
  * Created by Caxton on 2016/6/8.
  */
class MongodbManagerTest extends FlatSpec with Matchers {
  val mongodbManager = MongodbManager()
  val randomUser = new BigInteger(130, new SecureRandom()).toString(32).substring(0, 24)
  val weight = Random.nextInt(100) + 25
  val doc = Document(
    mongodbManager.KEY_USER -> randomUser,
    mongodbManager.KEY_WEIGHT -> weight,
    mongodbManager.KEY_STATUS -> "",
    mongodbManager.KEY_POSTED_AT -> Instant.now().toString)

  "query a non exist user" should "return empty" in {
    mongodbManager.findOne(mongodbManager.KEY_USER, randomUser) should be ("")
  }

  "query with non empty collection or inserted user" should "not return empty" in {
    mongodbManager.insertOne(doc)
    mongodbManager.find() should not be ("")
    mongodbManager.findOne(mongodbManager.KEY_USER, randomUser) should not be ("")
  }

  "the result of query a replaced user" should "include it user name" in {
    mongodbManager.replaceOne(doc)
    mongodbManager.findOne(mongodbManager.KEY_USER, randomUser) should include (randomUser)
  }

  "query the deleted user" should "return empty" in {
    mongodbManager.deleteOne(mongodbManager.KEY_USER, randomUser)
    mongodbManager.findOne(mongodbManager.KEY_USER, randomUser) should be ("")
  }
}
