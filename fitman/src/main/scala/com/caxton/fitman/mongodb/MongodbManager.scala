package com.caxton.fitman.mongodb

import java.util.concurrent.TimeUnit

import com.caxton.fitman.api.Weight
import com.caxton.fitman.mongodb.MongodbHelpers._
import com.mongodb.client.model.UpdateOptions
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
  val KEY_STATUS = "status"
  val KEY_POSTED_AT = "posted_at"

  val DEFAULT_AWAIT_TIMEOUT = 10
  val DEFAULT_AWAIT_TIME_UNIT = TimeUnit.SECONDS

  val mMongoClient: MongoClient = MongoClient(MONGODB_URI)
  val mDatabase: MongoDatabase = mMongoClient.getDatabase(MONGODB_DB_NAME)
  val mCollection: MongoCollection[Document] = mDatabase.getCollection(MONGODB_COLLECTION_USER)

  def find(): String = {
    val futureUsers: Future[Seq[Document]] = mCollection.find().projection(excludeId()).toFuture()
    val res: Seq[Document] = Await.result(futureUsers, Duration(DEFAULT_AWAIT_TIMEOUT, DEFAULT_AWAIT_TIME_UNIT))
    val stringBuilder = StringBuilder.newBuilder
    res.foreach(
      user => {
        stringBuilder.append(if(stringBuilder == "") "" else ", ")
        stringBuilder.append(user.toJson().toString)
      }
    )
    stringBuilder.toString()
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

  def replaceOne(document: Document): Unit = {
    try {
      val userObj: BsonString = document.get(KEY_USER).get.asInstanceOf[BsonString]
      val updateOpts = (new UpdateOptions()).upsert(true)
      mCollection.replaceOne(equal(KEY_USER, userObj.getValue), document, updateOpts).results()
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

  def getDocFromWeight(weight: Weight): Document = {
    Document(
      KEY_USER -> weight.user,
      KEY_WEIGHT -> weight.weight,
      KEY_STATUS -> weight.status,
      KEY_POSTED_AT -> weight.postedAt.toDate)
  }
}

object MongodbManager {
  private val mongodbManager = new MongodbManager

  def apply() = mongodbManager
}