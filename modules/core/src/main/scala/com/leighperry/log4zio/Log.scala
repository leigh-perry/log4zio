package com.leighperry.log4zio

import zio.{ IO, UIO, ZIO }

trait Log[E, A] {
  def log: Log.Service[E, A]
}

object Log {
  def log[E, A]: ZIO[Log[E, A], Nothing, Log.Service[E, A]] =
    ZIO.access[Log[E, A]](_.log)

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
  // TODO parameterise by Error to allow safe and unsafe versions?
  trait Service[E, A] {
    def error(s: => A): IO[E, Unit]
    def warn(s: => A): IO[E, Unit]
    def info(s: => A): IO[E, Unit]
    def debug(s: => A): IO[E, Unit]
  }

  //// Built-in implementations

  def console[E, A](prefix: Option[String]): IO[E, Log[E, A]] =
    make(TaggedStringLogMedium.console(prefix))

  def silent[A]: ZIO[Any, Nothing, Log[Nothing, A]] =
    make[Nothing, A](TaggedStringLogMedium.silent[A])

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
