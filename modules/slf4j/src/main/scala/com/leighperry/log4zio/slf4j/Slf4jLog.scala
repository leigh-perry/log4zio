package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.Log
import zio.{ IO, ZIO }

object Slf4jLog {

  def logger(prefix: Option[String]): IO[Throwable, Log[Throwable, String]] =
    ZIO
      .effect(org.slf4j.LoggerFactory.getLogger(getClass))
      .flatMap(slfLogger => Log.make[Throwable, String](Slf4jLogMedium.slf4j(prefix, slfLogger)))

  def safeLogger(prefix: Option[String]): IO[Nothing, Log[Nothing, String]] =
    ZIO
      .effect(org.slf4j.LoggerFactory.getLogger(getClass))
      .flatMap(slfLogger => Log.make[Nothing, String](Slf4jLogMedium.safeSlf4j(prefix, slfLogger)))
      .catchAll {
        _ =>
          // fallback on creation failure to console output
          for {
            fb <- Log.safeConsole[String](prefix)
            _ <- fb.log.warn("Error creating slf4j logger; falling back to tagged console")
          } yield fb
      }

}
