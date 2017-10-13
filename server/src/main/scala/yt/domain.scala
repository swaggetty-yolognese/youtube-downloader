package yt

import java.time.LocalDateTime

package object domain {

  case class YoutubeDlInvocation(
    executable: String,
    configLocationOption: String,
    configLocationFile: String,
    videoUrl: String,
    envDir: String
  )

  case class FileEntry(
    absPath: String,
    creationDate: LocalDateTime
  )

}
