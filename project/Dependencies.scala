import sbt._

object Dependencies {
  object Version {
    val kindProjectorVersion = "0.10.3"

    val slf4jApi = "1.7.29"
    val zio = "1.0.0-RC17"

    val scalacheck = "1.14.3"
    val logback = "1.2.3"
  }

  val slf4jApi = "org.slf4j" % "slf4j-api" % Version.slf4jApi
  val zio = "dev.zio" %% "zio" % Version.zio

  val scalacheck = "org.scalacheck" %% "scalacheck" % Version.scalacheck
  val logback = "ch.qos.logback" % "logback-classic" % Version.logback
}
