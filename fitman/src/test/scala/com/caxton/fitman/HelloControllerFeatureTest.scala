package com.caxton.fitman

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

/**
  * Created by Caxton on 2016/5/24.
  */
class HelloControllerFeatureTest extends FeatureTest{
  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    twitterServer = new FitmanServer)



  "Say Hello" in {
    server.httpGet(
      path = "/hello",
      andExpect = Status.Ok,
      withBody = "Fitman says hello"
    )
  }
}
