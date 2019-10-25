package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.Log
import zio.{ IO, ZIO }

object Slf4jLog {

  /**
   * Creates a conventional JVM-style logger output using SLF4J
   *
   * @param prefix an optional application-specific string that can be prepended to each log message
   */
  def loggerE(prefix: Option[String]): IO[Throwable, Log[Throwable, String]] =
    ZIO
      .effect(org.slf4j.LoggerFactory.getLogger(getClass))
      .flatMap(slfLogger => Log.make[Throwable, String](Slf4jLogMedium.slf4jE(prefix, slfLogger)))

  /**
   * Creates a conventional JVM-style logger output using SLF4J. In the event of error when
   * writing to SLF4J, this logger falls back to the standard console logger for output.
   *
   * @param prefix an optional application-specific string that can be prepended to each log message
   */
  def logger(prefix: Option[String]): IO[Nothing, Log[Nothing, String]] =
    ZIO
      .effect(org.slf4j.LoggerFactory.getLogger(getClass))
      .flatMap(slfLogger => Log.make[Nothing, String](Slf4jLogMedium.slf4j(prefix, slfLogger)))
      .catchAll {
        _ =>
          // fallback on creation failure to console output
          for {
            fb <- Log.console[String](prefix)
            _ <- fb.log.warn("Error creating slf4j logger; falling back to tagged console")
          } yield fb
      }

}
