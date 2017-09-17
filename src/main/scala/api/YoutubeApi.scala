package api

import akka.http.scaladsl.server.Directives._
import example.domain.{ YoutubeVideoUrl, YoutubeVideoUuid }
import util.JsonSupport

trait YoutubeApi extends JsonSupport {

  lazy val youtubeRoute = post {
    path("yt" / "video") {
      entity(as[YoutubeVideoUrl]) { videoUrl =>
        complete(yt.YoutubeService.downloadVideo(videoUrl.videoUrl))
      }
    }
  } ~ get {
    pathPrefix("yt" / "download" / Segment) { videoUuid =>
      pathEnd {
        yt.YoutubeService.getFilePath(videoUuid) match {
          case Some(absFilePath) => getFromFile(absFilePath)
          case None              => reject
        }
      }
    }
  }
}
