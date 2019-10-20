package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.{ Level, LogMedium, TaggedStringLogMedium }
import zio.ZIO

object Slf4jLogMedium {

  def slf4j(prefix: Option[String], slf: org.slf4j.Logger): LogMedium[(Level, () => String)] = {
    val logger =
      LogMedium[(Level, () => String)] {
        a =>
          val (level: Level, s: (() => String)) = a
          val result =
            level match {
              case Level.Error => ZIO.effect(slf.error(s()))
              case Level.Warn => ZIO.effect(slf.warn(s()))
              case Level.Info => ZIO.effect(slf.info(s()))
              case Level.Debug => ZIO.effect(slf.debug(s()))
            }
          result.catchAll(_ => TaggedStringLogMedium.console(prefix).log(a)) // fallback on write failure
      }

    // If prefix is required, contramap this logger with another
    // stage that handles prefix insertion into the logged string
    prefix.fold(logger) {
      sPrefix =>
        logger.contramap {
          case (level: Level, s: (() => String)) =>
            (level, () => s"$sPrefix: $s")
        }
    }

  }
}
