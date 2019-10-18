package com.leighperry.log4zio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import zio.{ UIO, ZIO }

// TODO update comment
/**
 * In this module:
 *  [A] is the type of logged message rendering, typically String, eg a String to be packaged and ultimately
 *  sent to a logging service
 *
 *  [B] is the output type of the logging medium, eg the packaging JSON to be sent to a logging service
 */
trait Log[A] {
  self =>

  def log: Log.Service[A]

  def contramap[B](f: B => A): Log[B] =
    new Log[B] {
      override def log: Log.Service[B] =
        self.log.contramap(f)
    }
}

object Log {
  def log[A]: ZIO[Log[A], Nothing, Log.Service[A]] =
    ZIO.access[Log[A]](_.log)

  /**
   * This interface assumes that the user doesn't want to experience logging failures. Logging is
   * most important under failure conditions, so it is best to log via a fallback mechanism rather than
   * fail altogether. Hence error type `Nothing`. It is the responsibility of `Service` implementations
   * to implement fallback behaviour.
   */
  trait Service[A] {
    self =>

    def log(ls: LogStep[A]): UIO[Unit]

    //// shortcuts

    def error(message: => A): UIO[Unit] =
      log(Log.error(message))

    def warn(message: => A): UIO[Unit] =
      log(Log.warn(message))

    def info(message: => A): UIO[Unit] =
      log(Log.info(message))

    def debug(message: => A): UIO[Unit] =
      log(Log.debug(message))

    //// combinators

    def contramap[B](f: B => A): Service[B] =
      new Service[B] {
        override def log(ls: LogStep[B]): UIO[Unit] =
          self.log(ls.map(f))
      }
  }

  def make[A](fOutput: LogStep[A] => UIO[Unit]): UIO[Log[A]] =
    ZIO.effectTotal {
      new Log[A] {
        override def log: Service[A] =
          new Service[A] {
            override def log(ls: LogStep[A]): UIO[Unit] =
              fOutput(ls)
          }
      }
    }

  //// Built-in implementations

  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  def rawConsole: UIO[Log[String]] =
    make {
      ls =>
        zio.console.Console.Live.console.putStrLn(ls.message())
    }

  /** Add standard logging timestamp etc on top of the raw console functionality */
  def console(prefix: Option[String]): UIO[Log[String]] =
    rawConsole.map {
      _.contramap(
        s =>
          "%s %-5s - %s%s".format(
            timestampFormat.format(LocalDateTime.now),
            "ls.level",
            prefix.fold("")(s => s"$s: "),
            s
          )
      )
    }

//  make {
//    ls =>
//      val str =
//        "%s %-5s - %s%s".format(
//          timestampFormat.format(LocalDateTime.now),
//          ls.level,
//          prefix.fold("")(s => s"$s: "),
//          ls.message()
//        )
//      zio.console.Console.Live.console.putStrLn(str)
//  }

  def silent: UIO[Log[String]] =
    make(_ => ZIO.unit)

  ////

  sealed trait LogStep[A] {
    self =>

    def message: () => A
    val level: String

    def map[B](f: A => B): LogStep[B] =
      new LogStep[B] {
        override def message: () => B =
          () => f(self.message())

        override val level: String =
          self.level
      }
  }

  final case class Error[A](message: () => A) extends LogStep[A] {
    override val level: String = "ERROR"
  }
  final case class Warn[A](message: () => A) extends LogStep[A] {
    override val level: String = "WARN"
  }
  final case class Info[A](message: () => A) extends LogStep[A] {
    override val level: String = "INFO"
  }
  final case class Debug[A](message: () => A) extends LogStep[A] {
    override val level: String = "DEBUG"
  }

  def error[A](message: => A): LogStep[A] =
    Error(() => message)

  def warn[A](message: => A): LogStep[A] =
    Warn(() => message)

  def info[A](message: => A): LogStep[A] =
    Info(() => message)

  def debug[A](message: => A): LogStep[A] =
    Debug(() => message)

}
