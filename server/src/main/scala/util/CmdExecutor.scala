package util

import java.io.{ BufferedReader, InputStreamReader }

import com.typesafe.scalalogging.LazyLogging
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object CmdHelper extends LazyLogging {

  case class StIO(
    stdOut: (String) => Unit = logger.info(_),
    stdErr: (String) => Unit = logger.error(_)
  )

  implicit class CmdExecutor(cmd: String) {

    def exec(onStdIO: StIO = StIO()): Future[Unit] = Future {
      logger.info(s"Executing $cmd")
      val proc = Runtime.getRuntime.exec(cmd)

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

