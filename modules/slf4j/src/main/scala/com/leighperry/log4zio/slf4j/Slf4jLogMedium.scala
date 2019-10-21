package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.{ Level, LogMedium, Tagged, TaggedStringLogMedium }
import zio.ZIO

object Slf4jLogMedium {

  def slf4j(prefix: Option[String], slf: org.slf4j.Logger): LogMedium[Tagged[String]] = {
    val logger =
      LogMedium[Tagged[String]] {
        a: Tagged[String] =>
          val result =
            a.level match {
              case Level.Error =>
                ZIO.effect(slf.error(a.message()))
              case Level.Warn =>
                ZIO.effect(slf.warn(a.message()))
              case Level.Info =>
                ZIO.effect(slf.info(a.message()))
              case Level.Debug =>
                ZIO.effect(slf.debug(a.message()))
            }
          result.catchAll(_ => TaggedStringLogMedium.console(prefix).log(a)) // fallback on write failure
      }

    // If prefix is required, contramap this logger with another
    // stage that handles prefix insertion into the logged string
    prefix.fold(logger) {
      sPrefix =>
        logger.contramap {
          a: Tagged[String] =>
            a.copy(message = () => s"$sPrefix: ${a.message()}")
        }
    }
  }
}
