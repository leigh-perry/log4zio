package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.{ Level, Log, LogMedium }
import zio.{ UIO, ZIO }

object Slf4jLog {

  //// Built-in implementations

  def logger(prefix: Option[String]): UIO[Log] = {
    val slf =
      for {
        slfLogger <- ZIO.effect(org.slf4j.LoggerFactory.getLogger(getClass))
        logger <- make(Slf4jLogMedium.slf4j(prefix, slfLogger))
      } yield logger

    slf.catchAll {
      _ =>
        // fallback on failure
        for {
          fb <- Log.console(prefix)
          _ <- fb.log.warn("Error creating slf4j logger; falling back to tagged console")
        } yield fb
    }
  }

  def make(logMedium: LogMedium[(Level, () => String)]): UIO[Log] =
    ZIO.succeed {
      new Log {
        override def log: Log.Service =
          new Log.Service {
            override def error(s: => String): UIO[Unit] =
              logMedium.log(Level.Error -> (() => s)) // TODO LogStep instead of (,) here?

            override def warn(s: => String): UIO[Unit] =
              logMedium.log(Level.Warn -> (() => s))

            override def info(s: => String): UIO[Unit] =
              logMedium.log(Level.Info -> (() => s))

            override def debug(s: => String): UIO[Unit] =
              logMedium.log(Level.Debug -> (() => s))
          }
      }
    }

  // TODO contramap with prefix

}
