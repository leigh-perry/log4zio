package com.leighperry.log4zio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import zio.{ UIO, ZIO }

/**
 * Encapsulation of log writing to some medium, via `A => UIO[Unit]`
 */
final case class LogMedium[A](log: A => UIO[Unit]) {

  def contramap[B](f: B => A): LogMedium[B] =
    LogMedium[B](b => log(f(b)))

  def contramapM[B](f: B => UIO[A]): LogMedium[B] =
    LogMedium[B](b => f(b).flatMap(log))

}

/**
 * Support for conventional JVM-style logging, ie tagged with level and timestamp
 */
trait Level { val name: String }
object Level {
  object Error extends Level { override val name = "ERROR" }
  object Warn extends Level { override val name = "WARN" }
  object Info extends Level { override val name = "INFO" }
  object Debug extends Level { override val name = "DEBUG" }
}

//// Log medium writer implementations

object RawLogMedium {

  def console: LogMedium[String] =
    LogMedium[String](zio.console.Console.Live.console.putStrLn)

}

final case class Tagged[A](level: Level, message: () => A)

object TaggedStringLogMedium {
  final case class TimestampedMessage[A](level: Level, message: () => A, timestamp: String)

  def console[A](prefix: Option[String]): LogMedium[Tagged[A]] =
    withTags(prefix, RawLogMedium.console)

  def silent[A]: LogMedium[Tagged[A]] =
    LogMedium(_ => ZIO.unit)

  def withTags[A](prefix: Option[String], base: LogMedium[String]): LogMedium[Tagged[A]] =
    base.contramap {
      m: TimestampedMessage[A] =>
        "%s %-5s - %s%s".format(
          m.timestamp,
          m.level.name,
          prefix.fold("")(s => s"$s: "),
          m.message()
        )
    }.contramapM {
      a: Tagged[A] =>
        ZIO
          .effect(LocalDateTime.now)
          .map(timestampFormat.format)
          .catchAll(_ => UIO("(timestamp error)"))
          .map(TimestampedMessage[A](a.level, a.message, _))
    }

  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

}
