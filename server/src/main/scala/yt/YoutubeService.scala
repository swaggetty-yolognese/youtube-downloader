package yt

import java.io.{ File, FileNotFoundException }
import java.nio.file.{ Files, Paths }
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import example.AppEntryPoint.system
import util.CmdHelper._
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.Future

object YoutubeService extends LazyLogging {

  implicit val dispatcher = system.dispatcher

  system.scheduler.schedule(initialDelay = 30 seconds, interval = 5 minutes) {
    val storedFilePaths = Files.list(Paths.get(downloadFolder)).iterator.asScala.map(_.toAbsolutePath.toString).toSet
    val nonServedFilePaths = uuidToFullPath.values.toSet

    val toBeRemovedPaths = storedFilePaths -- nonServedFilePaths
    logger.info(s"Deleting ${toBeRemovedPaths.size} mp3 files")
    toBeRemovedPaths foreach { path =>
      new File(path).delete
    }

  }

  val uuidToFullPath = scala.collection.mutable.Map[String, String]()
  lazy val config = com.typesafe.config.ConfigFactory.load()

  final private val downloadFolder = config.getString("application.downloadFolder")
  final private val filenamePrefix = config.getString("application.fileNamePrefix")
  final private val fileNameRegex = s"$downloadFolder\\/$filenamePrefix-[a-zA-z0-9\\w\\-\\_]+.mp3".r //FIXME enforce max size of youtube-id

  final private val youtubeDlOptions = s"-x --audio-format mp3 -o $downloadFolder/$filenamePrefix-%(id)s.mp3"

  //Regex matchers
  final private val downloadPercentaceRegex = "(\\d+\\.\\d)\\%".r

  def downloadVideo(videoUrl: String): Future[String] = {
    var fileName = ""

    val downloadF = s"youtube-dl $youtubeDlOptions $videoUrl".exec(StIO(stdOut = { out =>
      downloadPercentaceRegex.findFirstIn(out) map { matchedDownloadPerc =>
        logger.info(s"Download $matchedDownloadPerc")
      }

      fileNameRegex.findFirstIn(out).map { matchedFilename =>
        fileName = matchedFilename
      }

    }))

    downloadF.map { _ =>

      if (fileName == "")
        throw new IllegalArgumentException("Filename not found")

      val outFile = new File(fileName)

      if (!outFile.exists)
        throw new FileNotFoundException("Downloaded file does not exist")

      val uuid = UUID.randomUUID().toString

      logger.debug(s"storing ($uuid, ${outFile.getAbsolutePath}")
      uuidToFullPath += uuid -> outFile.getAbsolutePath

      uuid

    }

  }

  def getFilePath(uuid: String): Option[String] = {
    uuidToFullPath.remove(uuid)
  }

}
