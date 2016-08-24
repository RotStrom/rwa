package actors

import akka.actor.{Actor, ActorRef, Props}
import play.api.{Play, Logger}
import play.api.libs.iteratee.{Iteratee, Enumeratee, Concurrent, Enumerator}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}
import play.api.libs.ws.WS
import play.extras.iteratees.{JsonIteratees, Encoding}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

class TwitterStreamer(out: ActorRef) extends Actor {
  def receive = {
    case "subscribe" ⇒
      Logger.info("Received subscription from a client")
      TwitterStreamer.subscribe(out)
  }
}

object TwitterStreamer {
  def props(out: ActorRef) = Props(new TwitterStreamer(out))

  private var broadcastEnumerator: Option[Enumerator[JsObject]] = None

  def connect(): Unit = {
    credentials.map { case (consumerKey, requestToken) ⇒
      val (iteratee, enumerator) = Concurrent.joined[Array[Byte]]

      val jsonStream: Enumerator[JsObject] = enumerator &>
        Encoding.decode() &>
        Enumeratee.grouped(JsonIteratees.jsSimpleObject)

      val (be, _) = Concurrent.broadcast(jsonStream)
      broadcastEnumerator = Some(be)

      val maybeMasterNodeUrl = Option(System.getProperty("masterNodeUrl"))
      val url = maybeMasterNodeUrl.getOrElse("https://stream.twitter.com/1.1/statuses/filter.json")

      WS
        .url(url)
        .sign(OAuthCalculator(consumerKey, requestToken))
        .withQueryString("track" → "cat")
        .get { response ⇒
          Logger.info("Status: " + response.status)
          iteratee
        }.map { _ ⇒
        Logger.info("Twitter stream closed")
      }
    } getOrElse {
      Logger.error("Twitter credentials missing")
    }
  }

  def credentials: Option[(ConsumerKey, RequestToken)] = for {
    apiKey ← Play.configuration.getString("twitter.apiKey")
    apiSecret ← Play.configuration.getString("twitter.apiSecret")
    token ← Play.configuration.getString("twitter.token")
    tokenSecret ← Play.configuration.getString("twitter.tokenSecret")
  } yield (ConsumerKey(apiKey, apiSecret), RequestToken(token, tokenSecret))

  def subscribe(out: ActorRef): Unit = {
    if (broadcastEnumerator.isEmpty) {
      connect()
    }
    val twitterClient = Iteratee.foreach[JsObject](t ⇒ out ! t)
    broadcastEnumerator.foreach(enumerator ⇒ enumerator run twitterClient)
  }

  def subscribeNode: Enumerator[JsObject] = {
    if (broadcastEnumerator.isEmpty) connect()
    broadcastEnumerator.getOrElse(Enumerator.empty[JsObject])
  }
}
