package com.leighperry.log4zio

import zio.{ IO, ZIO }

trait Log[E, A] {
  def log: Log.Service[E, A]
}

object Log {

  /** Synonym for a non-failing log service – one that does not emit errors */
  type SafeLog[A] = Log[Nothing, A]

  /** Shortcut to retrieving the configured `Log.Service` */
  def log[E, A]: ZIO[Log[E, A], Nothing, Log.Service[E, A]] =
    ZIO.access[Log[E, A]](_.log)

  /** Shortcut to retrieving the configured non-failing string-based `Log.Service` */
  def stringLog: ZIO[Log[Nothing, String], Nothing, Log.Service[Nothing, String]] =
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

  /** A console with conventional JVM-style logger output that can emit `Throwable` errors */
  def consoleE[A](prefix: Option[String]): IO[Throwable, Log[Throwable, A]] =
    make[Throwable, A](TaggedLogMedium.consoleE(prefix))

  /**
   * An unfailing console with conventional JVM-style logger output that falls back to
   *  simple console output in the event of any error
   */
  def console[A](prefix: Option[String]): IO[Nothing, Log[Nothing, A]] =
    make[Nothing, A](TaggedLogMedium.console(prefix))

  /** Inhibit log output – useful for unit testing */
  def silent[A]: ZIO[Any, Nothing, Log[Nothing, A]] =
    make[Nothing, A](TaggedLogMedium.silent[A])

  // TODO `LogMedium` could be `R` here
  def make[E, A](logMedium: LogMedium[E, Tagged[A]]): IO[E, Log[E, A]] =
    ZIO.succeed {
      new Log[E, A] {
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
