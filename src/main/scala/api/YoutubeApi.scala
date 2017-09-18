package api

import akka.http.scaladsl.server.Directives._
import example.domain.YoutubeVideoUrl
import util.JsonSupport
import yt.YoutubeService

trait YoutubeApi extends JsonSupport {

  lazy val youtubeRoute = post {
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
