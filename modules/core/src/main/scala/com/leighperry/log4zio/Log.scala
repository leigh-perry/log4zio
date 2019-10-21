package com.leighperry.log4zio

import zio.{ UIO, ZIO }

trait Log[A] {
  def log: Log.Service[A]
}

object Log {
  def log[A]: ZIO[Log[A], Nothing, Log.Service[A]] =
    ZIO.access[Log[A]](_.log)

  def stringLog: ZIO[Log[String], Nothing, Log.Service[String]] =
    log[String]

  /**
   * An implementation of conventional logging with timestamp and logging level
   *
   * This interface assumes that the user doesn't want to experience logging failures. Logging is
   * most important under failure conditions, so it is best to log via a fallback mechanism rather than
   * fail altogether. Hence error type `Nothing`. It is the responsibility of `Service` implementations
   * to implement fallback behaviour.
   */
  trait Service[A] {
    def error(s: => A): UIO[Unit]
    def warn(s: => A): UIO[Unit]
    def info(s: => A): UIO[Unit]
    def debug(s: => A): UIO[Unit]
  }

  //// Built-in implementations

  def console[A](prefix: Option[String]): UIO[Log[A]] =
    make(TaggedStringLogMedium.console(prefix))

  def silent[A]: ZIO[Any, Nothing, Log[A]] =
    make[A](TaggedStringLogMedium.silent[A])

  def make[A](logMedium: LogMedium[Tagged[A]]): UIO[Log[A]] =
    ZIO.succeed {
      new Log[A] {
        override def log: Service[A] =
          new Service[A] {
            override def error(s: => A): UIO[Unit] =
              write(Level.Error, s)
            override def warn(s: => A): UIO[Unit] =
              write(Level.Warn, s)
            override def info(s: => A): UIO[Unit] =
              write(Level.Info, s)
            override def debug(s: => A): UIO[Unit] =
              write(Level.Debug, s)

            private def write(level: Level, s: => A) =
              logMedium.log(Tagged(level, (() => s)))
          }
      }
    }

}
