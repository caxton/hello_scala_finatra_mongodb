package com.caxton.fitman.api

import java.math.BigInteger
import java.security.SecureRandom

import com.caxton.fitman.FitmanServer
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import org.joda.time.Instant

import scala.util.Random

/**
  * Created by Caxton on 2016/5/26.
  */
class WeightResourceFeatureTest extends FeatureTest {
  override val server = new EmbeddedHttpServer(
    twitterServer = new FitmanServer
  )

  "WeightResourceFeatureTest" should {
    "Save user weight when POST request is made" in {
      server.httpPost(
        path = "/weights",
        postBody =
          """
            |{
            |"user":"caxton",
            |"weight":85,
            |"status":"Feeling great!!!"
            |}
          """.stripMargin,
        andExpect = Status.Created,
        withLocation = "/weights/caxton"
      )
    }

    "List specific weight for a user when GET request is made" in {
      val response = server.httpPost(
        path = "/weights",
        postBody =
          """
            |{
            |"user":"test_user_1",
            |"weight":80,
            |"posted_at" : "2016-01-03T14:34:06.871Z"
            |}
          """.stripMargin,
        andExpect = Status.Created
      )

      server.httpGetJson[List[Weight]](
        path = response.location.get,
        andExpect = Status.Ok,
        withJsonBody =
          """
            |[
            |  {
            |    "user" : "test_user_1",
            |    "weight" : 80,
            |    "posted_at" : "2016-01-03T14:34:06.871Z"
            |  }
            |]
          """.stripMargin
      )
    }

    "Bad request when user is not present in request" in {
      server.httpPost(
        path = "/weights",
        postBody =
          """
            |{
            |"weight":85
            |}
          """.stripMargin,
        andExpect = Status.BadRequest
      )
    }

    "Bad request when data not in range" in {
      server.httpPost(
        path = "/weights",
        postBody =
          """
            |{
            |"user":"testing12345678910908980898978798797979789",
            |"weight":250
            |}
          """.stripMargin,
        andExpect = Status.BadRequest,
        withErrors = Seq(
          "user: size [42] is not between 1 and 25",
          "weight: [250] is not between 25 and 200"
        )
      )
    }

    val randomUser = new BigInteger(130, new SecureRandom())
      .toString(32).substring(0, 24)

    "Add an user when POST request is made (MongoDB)" in {
      val randomTime = Instant.now()

      val response = server.httpPost(
        path = "/mongodb/weights",
        postBody =
          s"""
             |{
             |"user":"${randomUser}",
             |"weight":100,
             |"posted_at" : "${randomTime}"
             |}
          """.stripMargin,
        andExpect = Status.Created
      )

      server.httpGetJson[List[Weight]](
        path = response.location.get,
        andExpect = Status.Ok,
        withJsonBody =
          s"""
             |[
             |  {
             |    "user" : "${randomUser}",
             |    "weight" : 100
             |  }
             |]
          """.stripMargin
      )
    }

//    "Update user weight when PATCH request is made (MongoDB)" in {
//      val randomWeight = Random.nextInt(100) + 25
//      server.httpPatchJson(
//        path = "/mongodb/weights",
//        patchBody =
//          s"""
//             |{
//             |"user":"${randomUser}",
//             |"weight":${randomWeight}
//             |}
//          """.stripMargin,
//        andExpect = Status.Ok
//      )
//
//      server.httpGetJson[List[Weight]](
//        path = s"/mongodb/weights/:${randomUser}",
//        andExpect = Status.Ok,
//        withJsonBody =
//          s"""
//             |[
//             |  {
//             |    "user" : "${randomUser}",
//             |    "weight" : 75
//             |  }
//             |]
//          """.stripMargin
//      )
//    }

    "Update an user when PUT request is made (MongoDB)" in {
      val randomTime = Instant.now()
      val randomWeight = Random.nextInt(100) + 25
      server.httpPut(
        path = "/mongodb/weights",
        putBody =
          s"""
             |{
             |"user":"${randomUser}",
             |"weight":${randomWeight},
             |"posted_at" : "${randomTime}"
             |}
          """.stripMargin,
        andExpect = Status.Ok
      )

      server.httpGetJson[List[Weight]](
        path = s"/mongodb/weights/${randomUser}",
        andExpect = Status.Ok,
        withJsonBody =
          s"""
             |[
             |  {
             |    "user" : "${randomUser}",
             |    "weight" : ${randomWeight}
             |  }
             |]
          """.stripMargin
      )
    }

    "Delete an user (MongoDB)" in {
      server.httpDelete(
        path = s"/mongodb/weights/${randomUser}",
        andExpect = Status.Ok
      )

      server.httpGet(
        path = s"/mongodb/weights/${randomUser}",
        andExpect = Status.Ok,
        withBody =
          "[]"
      )
    }
  }
}
