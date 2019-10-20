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

  def console[A]: LogMedium[A] =
    LogMedium[A](a => zio.console.Console.Live.console.putStrLn(a.toString)) // TODO toString?

}

object TaggedStringLogMedium {
  final case class TimestampedMessage[A](message: () => A, level: Level, timestamp: String)

  def console(prefix: Option[String]): LogMedium[(Level, () => String)] =
    RawLogMedium.console // TODO take as arg so can layer on any logmedium, and rename `console`
    .contramap {
      m: TimestampedMessage[String] =>
        "%s %-5s - %s%s".format(
          m.timestamp,
          m.level.name,
          prefix.fold("")(s => s"$s: "),
          m.message()
        )
    }.contramapM {
      case (level: Level, message: (() => String)) =>
        ZIO
          .effect(LocalDateTime.now)
          .map(timestampFormat.format)
          .catchAll(_ => UIO("(timestamp error)"))
          .map(TimestampedMessage(message, level, _))
    }

  def silent: LogMedium[(Level, () => String)] =
    LogMedium(_ => ZIO.unit)

  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

}
