package demoblaze

import io.gatling.core.Predef._
import io.gatling.core.feeder.BatchableFeederBuilder
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.action.cookie.AddCookieDsl
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.util.Random


class DemoBlazeSimulation extends Simulation {
  val httpStatusSuccess = 200
  val productsFeeder: BatchableFeederBuilder[String]#F = csv("data/demoblaze.csv").circular

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl("https://api.demoblaze.com")
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en-GB,en-US;q=0.9,en;q=0.8")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36")

  object ProductView {
    val productView: ChainBuilder = feed(productsFeeder)
      .exec(http("Load product page '${product}' with id '${id}'")
        .post("/view")
        .body(StringBody("""{"id":"${id}"}""")).asJson
        .check(status.is(httpStatusSuccess)))
  }

  object AddToCart {
    val addToCart: ChainBuilder = exec(http("Add '${product}' to cart")
      .post("/addtocart")
      .body(StringBody(
        """{"id":"e9558f2b-8cb3-440f-f0d9-42c0adaa7c63",
          |"cookie":"user=d08c15f2-20a9-133d-dcb3-21e71ecc3c98",
          |"prod_id":${id},
          |"flag":false}""".stripMargin)).asJson
      .check(status.is(httpStatusSuccess)))
  }

  object DeleteCartWithUnauthenticatedUser {
    val deleteCart: ChainBuilder = exec(http("Delete cart with unauthenticated user")
      .post("/deletecart")
      .body(StringBody("""{"cookie": "user=d08c15f2-20a9-133d-dcb3-21e71ecc3c98"}""")).asJson
      .check(status.is(httpStatusSuccess)))
  }

  object DeleteCartWithAuthenticatedUser {
    val cookie: AddCookieDsl = Cookie("cookie", "demo")
    val token: AddCookieDsl = Cookie("tokenp_", "ZGVtbzE2MTEwNTc=")

    val deleteCart: ChainBuilder = exec(addCookie(cookie))
      .exec(addCookie(token))
      .exec(http("Delete cart with authenticated user")
        .post("/deletecart")
        .body(StringBody("""{"cookie": "user=demo"}""".stripMargin)).asJson
        .check(status.is(httpStatusSuccess)))
  }

  object SignUp {
    val randUsername: String = Random.nextString(5)
    val randPassword: String = Random.nextString(5)

    val signUp: ChainBuilder = exec(http("Sign up")
      .post("/signup")
      .body(StringBody(
        s"""{"username": "${randUsername}",
          |"password": "${randPassword}"}""".stripMargin)).asJson
      .check(status.is(httpStatusSuccess)))
  }

  object LogIn {
    val usernameAndPassword = "demo"

    val logIn: ChainBuilder = exec(http("Log in")
      .post("/login")
      .body(StringBody(
        s"""{"username": "${usernameAndPassword}",
          |"password": "${usernameAndPassword}"}""".stripMargin)).asJson
      .check(status.is(httpStatusSuccess)))
  }

  val users: ScenarioBuilder = scenario("Users")
    .exec(ProductView.productView, AddToCart.addToCart,
      DeleteCartWithUnauthenticatedUser.deleteCart, DeleteCartWithAuthenticatedUser.deleteCart,
      LogIn.logIn, SignUp.signUp)

  setUp(
    users.inject(
      nothingFor(5), // hold/stop for 5 seconds without doing anything
      atOnceUsers(1), // add 1 user once (1 user will start after 5 seconds)
      rampUsers(5) during (10), // ramp up to 5 users over the course of 10 seconds

    ).protocols(httpProtocol))
}
