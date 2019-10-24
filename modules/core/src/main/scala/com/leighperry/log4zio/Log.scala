package com.leighperry.log4zio

import zio.{ IO, ZIO }

trait LogE[E, A] {
  def log: LogE.Service[E, A]
}

object LogE {
  def log[E, A]: ZIO[LogE[E, A], Nothing, LogE.Service[E, A]] =
    ZIO.access[LogE[E, A]](_.log)

  def stringLog: ZIO[LogE[Nothing, String], Nothing, LogE.Service[Nothing, String]] =
    log[Nothing, String]

  /**
   * An implementation of conventional logging with timestamp and logging level
   *
   * This interface assumes that the user doesn't want to experience logging failures. Logging is
   * most important under failure conditions, so it is best to log via a fallback mechanism rather than
   * fail altogether. Hence error type `Nothing`. It is the responsibility of `Service` implementations
   * to implement fallback behaviour.
   */
  trait Service[E, A] {
    def error(s: => A): IO[E, Unit]
    def warn(s: => A): IO[E, Unit]
    def info(s: => A): IO[E, Unit]
    def debug(s: => A): IO[E, Unit]
  }

  //// Built-in implementations

  def consoleE[A](prefix: Option[String]): IO[Throwable, LogE[Throwable, A]] =
    make[Throwable, A](TaggedLogMedium.consoleE(prefix))

  def console[A](prefix: Option[String]): IO[Nothing, LogE[Nothing, A]] =
    make[Nothing, A](TaggedLogMedium.console(prefix))

  def silent[A]: ZIO[Any, Nothing, LogE[Nothing, A]] =
    make[Nothing, A](TaggedLogMedium.silent[A])

  // TODO `LogMedium` could be `R` here
  def make[E, A](logMedium: LogMedium[E, Tagged[A]]): IO[E, LogE[E, A]] =
    ZIO.succeed {
      new LogE[E, A] {
        override def log: Service[E, A] =
          new Service[E, A] {
            override def error(s: => A): IO[E, Unit] =
              write(Level.Error, s)
            override def warn(s: => A): IO[E, Unit] =
              write(Level.Warn, s)
            override def info(s: => A): IO[E, Unit] =
              write(Level.Info, s)
            override def debug(s: => A): IO[E, Unit] =
              write(Level.Debug, s)

            private def write(level: Level, s: => A) =
              logMedium.log(Tagged(level, (() => s)))
          }
      }
    }

}
