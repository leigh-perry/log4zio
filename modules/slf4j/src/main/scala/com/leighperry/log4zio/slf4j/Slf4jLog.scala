package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.LogE
import zio.{ IO, ZIO }

object Slf4jLog {

  def loggerE(prefix: Option[String]): IO[Throwable, LogE[Throwable, String]] =
    ZIO
      .effect(org.slf4j.LoggerFactory.getLogger(getClass))
      .flatMap(slfLogger => LogE.make[Throwable, String](Slf4jLogMedium.slf4jE(prefix, slfLogger)))

  def logger(prefix: Option[String]): IO[Nothing, LogE[Nothing, String]] =
    ZIO
      .effect(org.slf4j.LoggerFactory.getLogger(getClass))
      .flatMap(slfLogger => LogE.make[Nothing, String](Slf4jLogMedium.slf4j(prefix, slfLogger)))
      .catchAll {
        _ =>
          // fallback on creation failure to console output
          for {
            fb <- LogE.console[String](prefix)
            _ <- fb.log.warn("Error creating slf4j logger; falling back to tagged console")
          } yield fb
      }

}
