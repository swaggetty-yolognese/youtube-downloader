package util

import java.io.{ BufferedReader, InputStreamReader }

import com.typesafe.scalalogging.LazyLogging
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object CmdHelper {

  implicit class CmdExecutor(cmd: String) extends LazyLogging {

    def exec(onOutput: (String) => Unit): Future[String] = Future {
      logger.debug(s"Executing $cmd")
      val proc = Runtime.getRuntime.exec(cmd)

      val reader = new BufferedReader(
        new InputStreamReader(proc.getInputStream)
      )

      reader.lines.iterator.asScala.foreach(onOutput)
      
      val exitValue = proc.waitFor
      if (exitValue != 0)
        throw new IllegalStateException(s"\'$cmd\' exited with code $exitValue")

      "YOLO"
    }

  }

}

