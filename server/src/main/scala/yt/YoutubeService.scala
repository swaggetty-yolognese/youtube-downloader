package yt

import java.io.{ File, FileNotFoundException, IOException }
import java.nio.file.{ Files, Paths }
import java.nio.file.StandardOpenOption._
import java.util.UUID
import com.typesafe.scalalogging.LazyLogging
import example.AppEntryPoint.system
import util.CmdHelper._
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.Future

object YoutubeService extends LazyLogging {

  case class YoutubeDlInvocation(
    executable: String,
    configLocationOption: String,
    configLocationFile: String,
    videoUrl: String
  )

  implicit val dispatcher = system.dispatcher

  system.scheduler.schedule(initialDelay = 30 seconds, interval = 5 minutes) {
    downloadCleanupRoutine
  }

  val uuidToFullPath = scala.collection.mutable.Map[String, String]()
  lazy val config = com.typesafe.config.ConfigFactory.load()

  final private val downloadDirName = config.getString("application.downloadFolder")
  final private val downloadDir = createDownloadDirIfNotExists(downloadDirName)
  final private val filenamePrefix = config.getString("application.fileNamePrefix")
  final private val youtubeDlExecutable = "/home/andrea/.local/bin/youtube-dl"
  final private val confFileAbsPath = writeConfigFile()

  //Regex matcher
  final private val downloadPercentaceRegex = "(\\d+\\.\\d)\\%".r
  final private val fileNameRegex = s"$downloadDirName\\/$filenamePrefix-[a-zA-z0-9\\w\\-\\_]+.mp3".r

  def downloadVideo(videoUrl: String): Future[String] = {
    var fileName = ""

    val ytCmd = YoutubeDlInvocation(
      executable = youtubeDlExecutable,
      configLocationOption = "--config-location",
      configLocationFile = confFileAbsPath,
      videoUrl = videoUrl
    )

    val downloadF = ytCmd.exec(StIO(stdOut = { out =>
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

  private def createDownloadDirIfNotExists(dirName: String): File = {

    val curDir = new File("")
    val downDir = new File(curDir.getAbsolutePath + File.separator + dirName)

    val created = downDir.createNewFile
    val canWrite = downDir.canWrite

    if (!created)
      logger.warn(s"$dirName dir already exists")
    else
      logger.info(s"Creating ${downDir.getAbsolutePath}")

    if (!canWrite)
      logger.error(s"Can't write in ${downDir.getAbsolutePath}")

    if (!created && !downDir.exists)
      throw new IOException(s"File does not exist, unable to create: ${downDir.getAbsolutePath}")

    logger.info(s"Download dir: ${downDir.getAbsolutePath}")
    downDir
  }

  def writeConfigFile() = {
    val file = Paths.get("youtube-dl.conf")

    val confFile = Files.write(file, youtubeDlConfigRaw.toCharArray.map(_.toByte), CREATE_NEW, WRITE)
    val absPath = confFile.toAbsolutePath

    logger.info(s"Written config file into $absPath")
    logger.info(youtubeDlConfigRaw)

    absPath.toString
  }

  lazy val youtubeDlConfigRaw =
    s"""
      |# Always extract audio
      |-x
      |
      |# Mp3
      |--audio-format mp3
      |
      |# Save all videos under Movies directory in your home directory
      |-o ${downloadDir.getAbsolutePath}/$filenamePrefix-%(id)s.mp3
    """.stripMargin

  def downloadCleanupRoutine = {
    val storedFilePaths = Files.list(Paths.get(downloadDir.getAbsolutePath)).iterator.asScala.map(_.toAbsolutePath.toString).toSet
    val nonServedFilePaths = uuidToFullPath.values.toSet

    val toBeRemovedPaths = storedFilePaths -- nonServedFilePaths
    logger.info(s"Deleting ${toBeRemovedPaths.size} mp3 files")
    toBeRemovedPaths foreach { path =>
      new File(path).delete
    }

  }

}
