import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.packager.docker.Cmd

import scalariform.formatter.preferences._

enablePlugins(JavaServerAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

lazy val akkaHttpVersion = "10.0.10"
lazy val akkaVersion    = "2.5.4"
lazy val json4sVersion    = "3.5.3"

lazy val baseDockerImageName = "yt_dl:alpine_jdk_ytDl"

lazy val buildBaseImage = taskKey[Unit]("Build base docker image")
buildBaseImage := {
  println("Building base image")
  s"docker image build -t $baseDockerImageName youtube_dl/" !
}

//Use this task to build the image
lazy val publishDocker = taskKey[Unit]("Publish the image of youtube_converter" +
  "r_api")
publishDocker := {
  buildBaseImage.value
  (publishLocal in Docker).value
}

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "swaggetty-yolognese",
      scalaVersion    := "2.12.3",
      version         := "0.0.9",
      mainClass in Compile := Some("example.AppEntryPoint"),
      ScalariformKeys.preferences := scalariformPref.value
    )),
    name := "youtube_converter_api",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"               % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"           % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"             % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"              % akkaVersion,
      "de.heikoseeberger" %% "akka-http-json4s"        % "1.+",

      "org.json4s"        %% "json4s-native"           % json4sVersion,
      "org.json4s"        %% "json4s-ext"              % json4sVersion,

      "org.specs2"        %% "specs2-core"             % "3.8.6" % Test,
      "org.specs2"        %% "specs2-mock"             % "3.8.6" % Test,
      "org.specs2"        %% "specs2-matcher-extra"    % "3.8.6" % Test,
      "com.typesafe.akka" %% "akka-http-testkit"       % akkaHttpVersion % Test,

      "ch.qos.logback"    %  "logback-classic"         % "1.1.2",
      "com.typesafe.scala-logging" %% "scala-logging"  % "3.5.+"

    )
  )

dockerBaseImage := baseDockerImageName
dockerExposedPorts := Seq(8080)
dockerCommands := dockerCommands.value ++ Seq(
  Cmd("RUN", "mkdir /opt/docker/downloads && chown daemon:daemon /opt/docker/downloads")
)

lazy val scalariformPref = Def.setting {
  ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DanglingCloseParenthesis, Preserve)
    .setPreference(CompactStringConcatenation, true)
}
