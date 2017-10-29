package util

import java.io.{ BufferedReader, File, InputStreamReader }

import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.concurrent.Future
import example.AppEntryPoint.system
import yt.domain.YoutubeDlInvocation

object CmdHelper extends LazyLogging {

  implicit val dispatcher = system.dispatcher

  case class StIO(
    stdOut: (String) => Unit = logger.debug(_),
    stdErr: (String) => Unit = logger.error(_)
  )

  implicit class CmdExecutor(cmd: YoutubeDlInvocation) {

    def exec(onStdIO: StIO = StIO()): Future[Unit] = Future {
      logger.info(s"Executing $cmd ")
      val pb = new ProcessBuilder(
        cmd.executable,
        cmd.configLocationOption,
        cmd.configLocationFile,
        cmd.videoUrl
      ).directory(new File("/"))

      val proc = pb.start()

      val stdOutReader = new BufferedReader(
        new InputStreamReader(proc.getInputStream)
      )

      val stdErrReader = new BufferedReader(
        new InputStreamReader(proc.getErrorStream)
      )

      //bind buffered reader's iterator to callback(s)
      stdOutReader.lines.iterator.asScala.foreach(onStdIO.stdOut)
      stdErrReader.lines.iterator.asScala.foreach(onStdIO.stdErr)

      val exitValue = proc.waitFor
      if (exitValue != 0)
        throw new IllegalStateException(s"\'$cmd\' exited with code $exitValue")

    }

  }

}

