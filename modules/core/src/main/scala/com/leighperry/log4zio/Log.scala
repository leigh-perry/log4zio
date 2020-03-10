package com.leighperry.log4zio

import zio.IO

/** A service for conventional logging with timestamp and logging level */
trait Log[E, A] {
  def error(s: => A): IO[E, Unit]
  def warn(s: => A): IO[E, Unit]
  def info(s: => A): IO[E, Unit]
  def debug(s: => A): IO[E, Unit]
}

object Log {

  /** Synonym for a non-failing log service – one that does not emit errors */
  type SafeLog[A] = Log[Nothing, A]

  //// Built-in implementations

  /** A console with conventional JVM-style logger output that can emit `Throwable` errors */
  def consoleE[A](prefix: Option[String]): IO[Throwable, Log[Throwable, A]] =
    make[Throwable, A](TaggedLogMedium.consoleE(prefix))

  /**
   * An unfailing console with conventional JVM-style logger output that falls back to
   *  simple console output in the event of any error
   */
  def console[A](prefix: Option[String]): IO[Nothing, Log[Nothing, A]] =
    make(TaggedLogMedium.console(prefix))

  /** Inhibit log output – useful for unit testing */
  def silent[A]: IO[Nothing, Log[Nothing, A]] =
    make(TaggedLogMedium.silent[A])

  def make[E, A](logMedium: LogMedium[E, Tagged[A]]): IO[E, Log[E, A]] =
    IO.succeed {
      new Log[E, A] {
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
