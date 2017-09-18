package yt

import java.io.{ File, FileNotFoundException }
import java.util.UUID
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext.Implicits.global
import util.CmdHelper._
import scala.concurrent.Future

object YoutubeService extends LazyLogging {

  val urlsToFile = scala.collection.mutable.Map[String, String]()

  final private val downloadFolder = "downloads"
  final private val filenamePrefix = "track"
  final private val fileNameRegex = s"$downloadFolder\\/$filenamePrefix-[a-zA-Z0-9]+.mp3".r

  final private val youtubeDlOptions = "--config-location /home/andrea/workspace/youtube_converter_api/youtube_dl.conf"

  def downloadVideo(videoUrl: String): Future[String] = {

    var fileName = ""

    val downloadF = s"youtube-dl $youtubeDlOptions $videoUrl".exec(StIO(stdOut = { out =>

      fileNameRegex.findFirstIn(out) match {
        case Some(matching) =>
          fileName = matching
        case None =>
      }

    }))

    downloadF.map { _ =>

      if (fileName == "")
        throw new IllegalArgumentException("Filename not found")

      val outFile = new File(fileName)

      if (!outFile.exists)
        throw new FileNotFoundException("Downloaded file could not found")

      val uuid = UUID.randomUUID().toString

      logger.info(s"storing ($uuid, ${outFile.getAbsolutePath}")
      urlsToFile += uuid -> outFile.getAbsolutePath

      uuid

    }

  }

  def getFilePath(uuid: String): File = {

    val file = urlsToFile.get(uuid) match {
      case None       => throw new IllegalArgumentException(s"UUID not found $uuid")
      case Some(path) => new File(path)
    }

    if (!file.exists)
      throw new IllegalStateException(s"File not fount for $uuid at ${urlsToFile.get(uuid).get}")

    //remove from map
    urlsToFile -= uuid

    //remove file after serving it
    file
  }

}
