package yt

import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext.Implicits.global
import util.CmdHelper._

object YoutubeService extends LazyLogging {

  val ytDlOptions = "--extract-audio --audio-format mp3"

  def getVideo(videoUrl: String) = {

    s" youtube-dl $ytDlOptions $videoUrl ".exec({ out =>
      logger.info(out)
    })

  }

}
