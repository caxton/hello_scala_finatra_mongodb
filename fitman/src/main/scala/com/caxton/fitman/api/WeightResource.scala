package com.caxton.fitman.api

import com.caxton.fitman.mongodb.MongodbManager
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.utils.FuturePools
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

  //see http://twitter.github.io/finatra/user-guide/build-new-http-server/controller.html#requests
  val mFuturePool = FuturePools.unboundedPool("CallbackConverter")

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
    mFuturePool {
      "[" + mongodbManager.find() + "]"
    }
  }

  get("/mongodb/weights/:user") { request: Request =>
    info( s"""finding weight for user ${request.params(KEY_USER)}""")
    mFuturePool {
      "[" + mongodbManager.findOne(KEY_USER, request.params(KEY_USER)) + "]"
    }
  }

  post("/mongodb/weights") {
    weight: Weight =>
      val doc = mongodbManager.getDocFromWeight(weight)
      mFuturePool {
        mongodbManager.insertOne(doc)
        response.created.location(s"/mongodb/weights/${weight.user}")
      }
  }


//  patch("/mongodb/weights") {
//    weight: Weight =>
//      val doc = mongodbManager.getDocFromWeight(weight)
//      mFuturePool {
//        mongodbManager.updateOne(doc)
//        response.ok
//      }
//  }

  put("/mongodb/weights") {
    weight: Weight =>
      val doc = mongodbManager.getDocFromWeight(weight)
      mFuturePool {
        val r = time(s"Total time take to post weight for user '${weight.user}' is %d ms(mongoDB)") {
          mongodbManager.replaceOne(doc)
          response.ok
        }
        r
      }
  }

  delete("/mongodb/weights/:user") { request: Request =>
    info( s"""delete a user ${request.params(KEY_USER)}""")
    mFuturePool {
      mongodbManager.deleteOne(KEY_USER, request.params(KEY_USER))
      response.ok
    }
  }
}

case class Weight(
                   @Size(min = 1, max = 25) user: String,
                   @Range(min = 25, max = 200) weight: Int,
                   status: Option[String],
                   postedAt: Instant = Instant.now()
                 )