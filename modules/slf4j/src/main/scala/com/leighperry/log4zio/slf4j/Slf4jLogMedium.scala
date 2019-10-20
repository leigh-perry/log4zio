package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.{ Level, LogMedium, TaggedStringLogMedium }
import zio.ZIO

object Slf4jLogMedium {

  def slf4j(prefix: Option[String], slf: org.slf4j.Logger): LogMedium[(Level, () => String)] =
    LogMedium {
      a =>
        val (level: Level, s: (() => String)) = a
        val result =
          level match {
            case Level.Error => ZIO.effect(slf.error(s()))  // TODO (contramap with) prefix
            case Level.Warn => ZIO.effect(slf.warn(s()))  // TODO (contramap with) prefix
            case Level.Info => ZIO.effect(slf.info(s()))  // TODO (contramap with) prefix
            case Level.Debug => ZIO.effect(slf.debug(s()))  // TODO (contramap with) prefix
          }
        result.catchAll(_ => TaggedStringLogMedium.console(prefix).log(a)) // fallback on write failure
    }
}
