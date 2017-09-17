package api

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import example.domain.Greeting
import util.JsonSupport

trait YoutubeApi extends JsonSupport {

  implicit def system: ActorSystem

  lazy val greetingRoute: Route = get {
    path("yt" / "video" / Segment) { videoUrl =>
      complete("OK")
    }
  }
}
