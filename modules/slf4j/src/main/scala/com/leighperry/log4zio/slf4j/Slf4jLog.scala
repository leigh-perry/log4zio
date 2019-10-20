package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.Log
import zio.{ UIO, ZIO }

object Slf4jLog {

  //// Built-in implementations

  def logger(prefix: Option[String]): UIO[Log] = {
    val log: ZIO[Any, Throwable, Log] =
      for {
        slfLogger <- ZIO.effect(org.slf4j.LoggerFactory.getLogger(getClass))
        logger <- Log.make(Slf4jLogMedium.slf4j(prefix, slfLogger))
      } yield logger

    log.catchAll {
      _ =>
        // fallback on failure
        for {
          fb <- Log.console(prefix)
          _ <- fb.log.warn("Error creating slf4j logger; falling back to tagged console")
        } yield fb
    }
  }

}
