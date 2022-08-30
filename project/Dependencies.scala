import sbt._

object Dependencies {
  object Version {
    val kindProjectorVersion = "0.10.3"

    val slf4jApi = "1.7.36"
    val zio = "1.0.16"

    val logback = "1.4.0"
  }

  val slf4jApi = "org.slf4j" % "slf4j-api" % Version.slf4jApi
  val zio = "dev.zio" %% "zio" % Version.zio

  val logback = "ch.qos.logback" % "logback-classic" % Version.logback

  val zioTest = "dev.zio" %% "zio-test" % Version.zio
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Version.zio
}
