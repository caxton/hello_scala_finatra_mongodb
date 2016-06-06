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
  val MONGODB_URI = "mongodb://localhost/";
  val MONGODB_DB_NAME = "finatra_user"
  val MONGODB_COLLECTION_USER = "user"

  val KEY_USER = "user"
  val KEY_WEIGHT = "weight"

  val DEFAULT_AWAIT_TIMEOUT = 10
  val DEFAULT_AWAIT_TIME_UNIT = TimeUnit.SECONDS

  val mMongoClient: MongoClient = MongoClient(MONGODB_URI)
  val mDatabase: MongoDatabase = mMongoClient.getDatabase(MONGODB_DB_NAME)
  val mCollection: MongoCollection[Document] = mDatabase.getCollection(MONGODB_COLLECTION_USER)

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
    var result: String = new String("");
    try {
      result = mCollection.find(equal(tag, value))
        .projection(fields(include(KEY_USER, KEY_WEIGHT), excludeId())).headResult().toJson()
    } catch {
      case ex: IllegalStateException => {
        println(ex.getMessage)
      }
    }
    result
  }

  def insertOne(document: Document): Unit = {
    mCollection.insertOne(document).results()
  }

  def updateOne(document: Document): Unit = {
    try {
      val userObj: BsonString = document.get(KEY_USER).get.asInstanceOf[BsonString]
      val weightObj: BsonString = document.get(KEY_WEIGHT).get.asInstanceOf[BsonString]
      mCollection.updateOne(equal(KEY_USER, userObj.getValue), set(KEY_WEIGHT, weightObj.getValue)).results()
    }
    catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }
  }

  def updateOne(document: Document, upTag: String, upValue: Int): Unit = {
    try {
      val userObj: BsonString = document.get(KEY_USER).get.asInstanceOf[BsonString]
      mCollection.updateOne(equal(KEY_USER, userObj.getValue), set(upTag, upValue)).results()
    }
    catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }
  }

  def updateOne(document: Document, upTag: String, upValue: String): Unit = {
    try {
      val userObj: BsonString = document.get(KEY_USER).get.asInstanceOf[BsonString]
      mCollection.updateOne(equal(KEY_USER, userObj.getValue), set(upTag, upValue)).results()
    }
    catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }
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