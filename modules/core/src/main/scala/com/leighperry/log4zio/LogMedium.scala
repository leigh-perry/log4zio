package com.leighperry.log4zio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import zio.{ IO, UIO, ZIO }

/**
 * Encapsulation of log writing to some medium, via `A => UIO[Unit]`
 */
final case class LogMedium[E, A](log: A => IO[E, Unit]) {

  def contramap[B](f: B => A): LogMedium[E, B] =
    LogMedium[E, B](b => log(f(b)))

  def contramapM[E1 >: E, B](f: B => IO[E1, A]): LogMedium[E1, B] =
    LogMedium[E1, B](b => f(b).flatMap(log))

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

  def console: LogMedium[Nothing, String] =
    LogMedium[Nothing, String](zio.console.Console.Live.console.putStrLn)

}

final case class Tagged[A](level: Level, message: () => A)

object TaggedLogMedium {
  final case class TimestampedMessage[A](level: Level, message: () => A, timestamp: String)

  def console[E, A](prefix: Option[String]): LogMedium[E, Tagged[A]] =
    withTags(prefix, RawLogMedium.console)

  def silent[A]: LogMedium[Nothing, Tagged[A]] =
    LogMedium(_ => ZIO.unit)

  def withTags[E, A](
    prefix: Option[String],
    base: LogMedium[Nothing, String]
  ): LogMedium[E, Tagged[A]] =
    base
      .contramap[TimestampedMessage[A]](taggedTimestamped(prefix))
      .contramapM[E, Tagged[A]](tagged)

  def tagged[A, E]: Tagged[A] => ZIO[Any, Nothing, TimestampedMessage[A]] =
    (a: Tagged[A]) =>
      ZIO
        .effect(LocalDateTime.now)
        .map(timestampFormat.format)
        .catchAll(_ => UIO("(timestamp error)"))
        .map(TimestampedMessage[A](a.level, a.message, _))

  def taggedTimestamped[A](prefix: Option[String]): TimestampedMessage[A] => String =
    (m: TimestampedMessage[A]) =>
      "%s %-5s - %s%s".format(
        m.timestamp,
        m.level.name,
        prefix.fold("")(s => s"$s: "),
        m.message()
      )

  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

}
