package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.{ Level, LogMedium, Tagged, TaggedStringLogMedium }
import zio.ZIO

object Slf4jLogMedium {

  def slf4j(prefix: Option[String], slf: org.slf4j.Logger): LogMedium[Tagged[String]] = {
    val logger =
      LogMedium[Tagged[String]] {
        a: Tagged[String] =>
          val s = a.message
          val result =
            a.level match {
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
          a: Tagged[String] =>
            Tagged(a.level, () => s"$sPrefix: ${a.message()}")
        }
    }
  }
}
