package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.{ Level, LogMedium, TaggedStringLogMedium }
import zio.ZIO

object Slf4jLogMedium {

//  val slf: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def logger(prefix: Option[String], slf: org.slf4j.Logger): LogMedium[(Level, String)] =
    LogMedium {
      a =>
        val (level, s) = a
        val result =
          level match {
            case Level.Debug => ZIO.effect(slf.debug(s))
            case Level.Error => ZIO.effect(slf.error(s))
            case Level.Info => ZIO.effect(slf.info(s))
            case Level.Warn => ZIO.effect(slf.warn(s))
          }
        result.catchAll(_ => TaggedStringLogMedium.console(prefix).log(a)) // fallback on write failure
    }
}
