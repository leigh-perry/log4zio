package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.{ Level, LogMedium, Tagged, TaggedLogMedium }
import zio.ZIO

object Slf4jLogMedium {

  def slf4jE(prefix: Option[String], slf: org.slf4j.Logger): LogMedium[Throwable, Tagged[String]] =
    prefixLogger[Throwable](prefix, slf4jLogMedium(slf))

  def slf4j(prefix: Option[String], slf: org.slf4j.Logger): LogMedium[Nothing, Tagged[String]] =
    prefixLogger[Nothing](
      prefix,
      slf4jLogMedium(slf)
        .withFallback(TaggedLogMedium.console(prefix).log)
    )

  def slf4jLogMedium(slf: org.slf4j.Logger): LogMedium[Throwable, Tagged[String]] =
    LogMedium[Throwable, Tagged[String]] {
      a: Tagged[String] =>
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
    }

  // If prefix is required, contramap this logger with another
  // stage that handles prefix insertion into the logged string
  def prefixLogger[E](
    prefix: Option[String],
    logger: LogMedium[E, Tagged[String]]
  ): LogMedium[E, Tagged[String]] =
    prefix.fold(logger) {
      sPrefix =>
        logger.contramap {
          a: Tagged[String] =>
            a.copy(message = () => s"$sPrefix: ${a.message()}")
        }
    }

}
