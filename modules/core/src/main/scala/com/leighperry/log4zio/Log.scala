package com.leighperry.log4zio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import zio.ZIO

trait Log {
  def log: Log.Service
}

object Log {
  def log: ZIO[Log, Nothing, Log.Service] =
    ZIO.access[Log](_.log)

  /**
   * This interface assumes that the user doesn't want to experience logging failures. Logging is
   * most important under failure conditions, so it is best to log via a fallback mechanism rather than
   * fail altogether. Hence error type `Nothing`. It is the responsibility of `Service` implementations
   * to implement fallback behaviour.
   */
  trait Service {
    def log: LogStep => ZIO[Any, Nothing, Unit]

    //// shortcuts

    def error(message: => String): ZIO[Any, Nothing, Unit] =
      log(Log.error(message))

    def warn(message: => String): ZIO[Any, Nothing, Unit] =
      log(Log.warn(message))

    def info(message: => String): ZIO[Any, Nothing, Unit] =
      log(Log.info(message))

    def debug(message: => String): ZIO[Any, Nothing, Unit] =
      log(Log.debug(message))
  }

  //// Built-in implementations

  def console(prefix: Option[String]): ZIO[Any, Nothing, Log] =
    ZIO.effectTotal {
      new Log {
        override def log: Service =
          new Service {
            val zioConsole = zio.console.Console.Live.console
            val timestampFormat: DateTimeFormatter =
              DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            val sPrefix = prefix.fold("")(s => s"$s: ")

            override def log: LogStep => ZIO[Any, Nothing, Unit] =
              l =>
                zioConsole.putStrLn(
                  "%s %-5s - %s%s"
                    .format(timestampFormat.format(LocalDateTime.now), l.level, sPrefix, l.message())
                )
          }
      }
    }

  def silent: ZIO[Any, Nothing, Log] =
    ZIO.effectTotal {
      new Log {
        override def log: Service =
          new Service {
            override def log: LogStep => ZIO[Any, Nothing, Unit] =
              _ => ZIO.unit
          }
      }
    }

  ////

  sealed trait LogStep {
    def message: () => String
    val level: String
  }

  final case class Error(message: () => String) extends LogStep {
    override val level: String = "ERROR"
  }
  final case class Warn(message: () => String) extends LogStep {
    override val level: String = "WARN"
  }
  final case class Info(message: () => String) extends LogStep {
    override val level: String = "INFO"
  }
  final case class Debug(message: () => String) extends LogStep {
    override val level: String = "DEBUG"
  }

  def error(message: => String): LogStep =
    Error(() => message)

  def warn(message: => String): LogStep =
    Warn(() => message)

  def info(message: => String): LogStep =
    Info(() => message)

  def debug(message: => String): LogStep =
    Debug(() => message)

}
