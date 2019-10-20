package com.leighperry.log4zio

import zio.{UIO, ZIO}

trait Log {
  def log: Log.Service
}

object Log {
  def log: ZIO[Log, Nothing, Log.Service] =
    ZIO.access[Log](_.log)

  /**
   * An implementation of conventional logging with timestamp and logging level
   *
   * This interface assumes that the user doesn't want to experience logging failures. Logging is
   * most important under failure conditions, so it is best to log via a fallback mechanism rather than
   * fail altogether. Hence error type `Nothing`. It is the responsibility of `Service` implementations
   * to implement fallback behaviour.
   */
  trait Service {
    def error(s: String): UIO[Unit]
    def warn(s: String): UIO[Unit]
    def info(s: String): UIO[Unit]
    def debug(s: String): UIO[Unit]
  }

  //// Built-in implementations

  def console(prefix: Option[String]): UIO[Log] =
    make(TaggedStringLogMedium.console(prefix))

  def silent: ZIO[Any, Nothing, Log] =
    make(TaggedStringLogMedium.silent)

  def make(logMedium: LogMedium[(Level, String)]): UIO[Log] =
    ZIO.succeed {
      new Log {
        override def log: Service =
          new Service {
            override def error(s: String): UIO[Unit] =
              logMedium.log(Level.Error -> s) // TODO LogStep instead of (,) here?

            override def warn(s: String): UIO[Unit] =
              logMedium.log(Level.Warn -> s)

            override def info(s: String): UIO[Unit] =
              logMedium.log(Level.Info -> s)

            override def debug(s: String): UIO[Unit] =
              logMedium.log(Level.Debug -> s)
          }
      }
    }

}
