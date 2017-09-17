package yt

import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext.Implicits.global
import util.CmdHelper._

object YoutubeService extends LazyLogging {

  def getVideo(videoUrl: String) = {

    s" youtube-dl -x $videoUrl ".exec.map { out =>
      logger.info(out+"\n")
    }

  }

}
