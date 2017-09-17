package yt

import java.io.{ File, FileNotFoundException }
import java.util.UUID
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext.Implicits.global
import util.CmdHelper._
import scala.concurrent.Future

object YoutubeService extends LazyLogging {

  val urlsToFile = scala.collection.mutable.Map[String, String]()

  final private val fileNameRegex = "[".r

  def downloadVideo(videoUrl: String): Future[String] = {

    var fileName = ""

    val downloadF = s"youtube-dl".exec(StIO(stdOut = { out =>

      out match {
        case fileNameRegex(_*) =>
          fileName = ""
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
      urlsToFile.put(uuid, outFile.getAbsolutePath)

      uuid

    }

  }

  def getFilePath(uuid: String): Option[String] = urlsToFile.get(uuid)

}
