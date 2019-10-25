package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.{ Level, LogMedium, Tagged, TaggedLogMedium }
import zio.ZIO

object Slf4jLogMedium {

  /**
   * Creates a conventional JVM-style logger output using SLF4J
   *
   * @param prefix an optional application-specific string that can be prepended to each log message
   */
  def slf4jE(prefix: Option[String], slf: org.slf4j.Logger): LogMedium[Throwable, Tagged[String]] =
    prefixLogger[Throwable](prefix, slf4jLogMedium(slf))

  /**
   * Creates a conventional JVM-style logger output using SLF4J. In the event of error when
   * writing to SLF4J, this logger falls back to the standard console logger for output.
   *
   * @param prefix an optional application-specific string that can be prepended to each log message
   */
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

  /**
   * If prefix is required, contramap this logger with another stage that handles prefix
   * insertion into the logged string
   */
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
