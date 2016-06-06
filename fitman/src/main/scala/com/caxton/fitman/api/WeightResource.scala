package com.caxton.fitman.api

import com.caxton.fitman.mongodb.MongodbManager
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.validation.{Range, Size}
import com.twitter.inject.Logging
import org.joda.time.Instant

import scala.collection.mutable

/**
  * Created by Caxton on 2016/5/26.
  */
class WeightResource extends Controller with Logging {
  val db = mutable.Map[String, List[Weight]]()
  val mongodbManager = MongodbManager()
  val KEY_USER = mongodbManager.KEY_USER

  get("/weights") { request: Request =>
    info("finding all weights for all users...")
    db
  }

  get("/weights/:user") { request: Request =>
    info( s"""finding weight for user ${request.params(KEY_USER)}""")
    db.getOrElse(request.params(KEY_USER), List())
  }

  post("/weights") {
    weight: Weight =>
      val r = time(s"Total time take to post weight for user '${weight.user}' is %d ms") {
        val weightsForUser = db.get(weight.user) match {
          case Some(weights) => weights :+ weight
          case None => List(weight)
        }
        db.put(weight.user, weightsForUser)
        response.created.location(s"/weights/${weight.user}")
      }
      r
  }

  get("/mongodb/weights") { request: Request =>
    info("finding all weights for all users...")
    "[" + mongodbManager.find() + "]"
  }

  get("/mongodb/weights/:user") { request: Request =>
    info( s"""finding weight for user ${request.params(KEY_USER)}""")
    "[" + mongodbManager.findOne(KEY_USER, request.params(KEY_USER)) + "]"
  }

  post("/mongodb/weights") {
    weight: Weight =>
      val r = time(s"Total time take to post weight for user '${weight.user}' is %d ms(mongoDB)") {
        val doc = mongodbManager.getDocFromWeight(weight)

        if (mongodbManager.findOne(KEY_USER, weight.user).isEmpty) {
          println("no matched record!")
          mongodbManager.insertOne(doc)
        }
        else {
          println("find matched record!")
          mongodbManager.updateOne(doc)
        }
        response.created.location(s"/weights/${weight.user}")
      }
      r
  }

  get("/mongodb/weights/del/:user") { request: Request =>
    info( s"""delete a user ${request.params(KEY_USER)}""")
    mongodbManager.deleteOne(KEY_USER, request.params(KEY_USER))
    response.ok()
  }
}

case class Weight(
                   @Size(min = 1, max = 25) user: String,
                   @Range(min = 25, max = 200) weight: Int,
                   status: Option[String],
                   postedAt: Instant = Instant.now()
                 )