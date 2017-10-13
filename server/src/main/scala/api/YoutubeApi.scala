package api

import java.nio.file.Paths

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{LoggingMagnet, RespondWithDirectives}
import scala.compat.java8.OptionConverters._
import scala.collection.immutable.Seq
import akka.http.scaladsl.model.HttpMethods._
import _root_.util.JsonSupport
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.scaladsl.FileIO
import com.typesafe.scalalogging.LazyLogging
import yt.YoutubeService

trait YoutubeApi extends JsonSupport with EnableCORSDirectives {

  lazy val youtubeRoute = logClientIP {
    enableCORS {
      doFuckingPreflight ~ ytRoute
    }
  }

  lazy val ytRoute = {
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

  private def serveFile(videoUuid: String) = complete {

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

  private def doFuckingPreflight = {
    options {
      pathPrefix("yt" / ("video" | "download")) {
        pathEnd {
          complete(HttpEntity.Empty)
        }
      }
    }
  }

}

trait EnableCORSDirectives extends RespondWithDirectives with LazyLogging {
  
  def headerLogger:LoggingMagnet[HttpRequest => Unit] = LoggingMagnet { loggingAdapter => request =>
    loggingAdapter.debug(s"Headers: ${request._3.toString()}")
    loggingAdapter.debug(s"HTTP Method: ${request._1}")
  }
  
  def logClientIP = extractClientIP map { remoteAddress =>
    logger.info(s"IP :${remoteAddress.getAddress.asScala.map(_.getHostAddress)}")
  }
  
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

