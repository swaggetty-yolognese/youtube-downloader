package api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RespondWithDirectives
import akka.http.scaladsl.model.headers.{ `Access-Control-Allow-Credentials`, `Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin` }
import akka.http.scaladsl.model.HttpMethods._
import example.domain.YoutubeVideoUrl
import util.JsonSupport
import yt.YoutubeService

trait YoutubeApi extends JsonSupport with EnableCORSDirectives {

  lazy val youtubeRoute = doFuckingPreflight ~ ytRoute

  lazy val ytRoute = enableCORS {
    post {
      path("yt" / "video") {
        entity(as[YoutubeVideoUrl]) { videoUrl =>
          complete(YoutubeService.downloadVideo(videoUrl.videoUrl))
        }
      }
    } ~ get {
      pathPrefix("yt" / "download" / Segment) { videoUuid =>
        pathEnd {
          getFromFile(YoutubeService.getFilePath(videoUuid))
        }
      }
    }
  }

  private def doFuckingPreflight = enableCORS {
    options {
      pathPrefix("yt" / ("video" | "download")) {
        pathEnd {
          complete("YOU SUCK")
        }
      }
    }
  }
}

trait EnableCORSDirectives extends RespondWithDirectives {

  private val allowedCorsVerbs = List(
    CONNECT, DELETE, GET, HEAD, OPTIONS,
    PATCH, POST, PUT, TRACE
  )

  private val allowedCorsHeaders = List(
    "X-Requested-With", "content-type", "origin", "accept"
  )

  lazy val enableCORS =
    respondWithHeader(`Access-Control-Allow-Origin`.`*`) &
      respondWithHeader(`Access-Control-Allow-Methods`(allowedCorsVerbs)) &
      respondWithHeader(`Access-Control-Allow-Headers`(allowedCorsHeaders)) &
      respondWithHeader(`Access-Control-Allow-Credentials`(true))
}
