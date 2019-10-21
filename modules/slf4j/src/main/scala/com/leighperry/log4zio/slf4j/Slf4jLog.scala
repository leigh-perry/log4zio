package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.Log
import zio.{ UIO, ZIO }

object Slf4jLog {

  def logger(prefix: Option[String]): UIO[Log[String]] =
    ZIO
      .effect(org.slf4j.LoggerFactory.getLogger(getClass))
      .flatMap(slfLogger => Log.make(Slf4jLogMedium.slf4j(prefix, slfLogger)))
      .catchAll {
        _ =>
          // fallback on creation failure to console output
          for {
            fb <- Log.console[String](prefix)
            _ <- fb.log.warn("Error creating slf4j logger; falling back to tagged console")
          } yield fb
      }

}
