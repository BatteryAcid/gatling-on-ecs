package nearmap

import io.gatling.commons.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class SampleSimulation extends Simulation {

  val httpConf = http.warmUp("http://gatling.io")
    .baseURL("http://gatling.io")
    .extraInfoExtractor(extraInfo => List(extraInfo.request, extraInfo.status, extraInfo.response.bodyLength))

  // Might consider warmup if initial tests indicates it will help
  // http://gatling.io/docs/2.1.4/http/http_request.html#silencing
  ///val warmupLoad = csv("warmup_load.csv").random
  val yourScenario = scenario("Do something")
    .exec(
      http("request")
        .get("/maps/")
        .queryParam("x", "1")
        .check(
          status.is(200)))

  setUp(
    yourScenario.inject(
      nothingFor(4 seconds), // 1
      atOnceUsers(10), // 2
      rampUsers(10) over (5 seconds), // 3
      constantUsersPerSec(20) during (15 seconds), // 4
      constantUsersPerSec(20) during (15 seconds) randomized, // 5
      rampUsersPerSec(10) to (20) during (10 minutes), // 6
      rampUsersPerSec(10) to (20) during (10 minutes) randomized, // 7
      splitUsers(1000) into (rampUsers(10) over (10 seconds)) separatedBy (10 seconds), // 8
      splitUsers(1000) into (rampUsers(10) over (10 seconds)) separatedBy (atOnceUsers(30)), // 9
      heavisideUsers(1000) over (20 seconds))).protocols(httpConf)

}
