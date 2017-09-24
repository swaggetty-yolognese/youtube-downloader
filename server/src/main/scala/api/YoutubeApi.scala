package api

import java.nio.file.Paths

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RespondWithDirectives
import scala.collection.immutable.Seq
import akka.http.scaladsl.model.HttpMethods._
import _root_.util.JsonSupport
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.scaladsl.{ FileIO }
import yt.YoutubeService

trait YoutubeApi extends JsonSupport with EnableCORSDirectives {

  lazy val youtubeRoute = doFuckingPreflight ~ ytRoute

  lazy val ytRoute = enableCORS {
    get {
      pathPrefix("yt" / "video") {
        path("convert") {
          parameter('url) { videoUrl =>
            complete(YoutubeService.downloadVideo(videoUrl))
          }
        } ~ path("download" / Segment) { videoUuid =>
          serveFile(videoUuid)
        }
      }
    }
  }
  

  def serveFile(videoUuid: String) = complete {

    YoutubeService.getFilePath(videoUuid) match {
      case None =>
        HttpResponse(StatusCodes.NotFound, entity = HttpEntity.Empty)
      case Some(filePath) =>
        val stream = FileIO.fromPath(Paths.get(filePath))
        val contentType = ContentType(MediaTypes.`audio/mpeg`)
        val ParsingResult.Ok(contentDispositionHeader, _) = HttpHeader.parse(
          "Content-Disposition",
          s"attachment; filename=${getFileName(filePath)}"
        )
        HttpResponse(
          StatusCodes.OK,
          headers = Seq(contentDispositionHeader),
          entity = HttpEntity(contentType, stream)
        )
    }
  }

  private def getFileName(filePath: String) =
    filePath
      .reverse
      .takeWhile(_ != '/')
      .reverse
      .mkString

  private def doFuckingPreflight = enableCORS {
    options {
      pathPrefix("yt" / ("video" | "download")) {
        pathEnd {
          complete(HttpEntity.Empty)
        }
      }
    }
  }

}

trait EnableCORSDirectives extends RespondWithDirectives {

  private val allowedCorsVerbs = List(
    GET, OPTIONS
  )

  private val allowedCorsHeaders = List(
    "content-type", "origin", "accept"
  )

  lazy val enableCORS =
    respondWithHeader(`Access-Control-Allow-Origin`.`*`) &
      respondWithHeader(`Access-Control-Allow-Methods`(allowedCorsVerbs)) &
      respondWithHeader(`Access-Control-Allow-Headers`(allowedCorsHeaders)) &
      respondWithHeader(`Access-Control-Allow-Credentials`(true))
}

