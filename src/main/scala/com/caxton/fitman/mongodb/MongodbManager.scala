package com.caxton.fitman.mongodb

import java.util.concurrent.TimeUnit

import com.caxton.fitman.mongodb.MongodbHelpers._
import com.twitter.finatra.utils.FuturePools
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Updates._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by Caxton on 2016/6/3.
  */
class MongodbManager private {
  val KEY_USER = "user"
  val KEY_WEIGHT = "weight"

  val DEFAULT_AWAIT_TIMEOUT = 10
  val DEFAULT_AWAIT_TIME_UNIT = TimeUnit.SECONDS

  val mMongoClient: MongoClient = MongoClient("mongodb://localhost/")
  val mDatabase: MongoDatabase = mMongoClient.getDatabase("finatra_user")
  val mCollection: MongoCollection[Document] = mDatabase.getCollection("user")

  val mFuturePool = FuturePools.unboundedPool("CallbackConverter")

  def find(): String = {
    val futureUsers: Future[Seq[Document]] = mCollection.find().projection(excludeId()).toFuture()
    val res: Seq[Document] = Await.result(futureUsers, Duration(DEFAULT_AWAIT_TIMEOUT, DEFAULT_AWAIT_TIME_UNIT))
    var results: String = new String("");
    res.foreach(
      user => results += user.toJson().toString
    )
    results
  }

  def findOne(tag: String, value: String): String = {
    val futureUsers: Future[Seq[Document]] = mCollection.find(equal(tag, value))
      .projection(fields(include(KEY_USER, KEY_WEIGHT), excludeId())).first().toFuture()
    val res: Seq[Document] = Await.result(futureUsers, Duration(DEFAULT_AWAIT_TIMEOUT, DEFAULT_AWAIT_TIME_UNIT))
    var result: String = new String("");
    if (res.length > 0) {
      res.foreach(
        res => result = res.toJson()
      )
    }
    result
  }

  def insertOne(document: Document): Unit = {
    mCollection.insertOne(document).results()
  }

  def updateOne(document: Document, upTag: String, upValue: Int): Unit = {
    val bsonObj: BsonString = document.get(KEY_USER).get.asInstanceOf[BsonString]
    mCollection.updateOne(equal(KEY_USER, bsonObj.getValue), set(upTag, upValue)).results()
  }

  def updateOne(document: Document, upTag: String, upValue: String): Unit = {
    val bsonObj: BsonString = document.get(KEY_USER).get.asInstanceOf[BsonString]
    mCollection.updateOne(equal(KEY_USER, bsonObj.getValue), set(upTag, upValue)).results()
  }

  def deleteOne(tag: String, value: String): Unit = {
    mCollection.deleteOne(equal(tag, value)).results()
  }

  def close(): Unit = {
    mMongoClient.close()
  }
}

object MongodbManager {
  private val mongodbManager = new MongodbManager
  def apply() = mongodbManager
}