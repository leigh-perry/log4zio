package com.leighperry.log4zio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import zio.{ IO, ZIO }

/**
 * Encapsulation of log writing to some medium, via `A => UIO[Unit]`
 */
final case class LogMedium[E, A](log: A => IO[E, Unit]) {
  def contramap[B](f: B => A): LogMedium[E, B] =
    LogMedium[E, B](b => log(f(b)))

  def contramapM[E1 >: E, B](f: B => IO[E1, A]): LogMedium[E1, B] =
    LogMedium[E1, B](b => f(b).flatMap(log))

  def withFallback[E1](fb: A => IO[E1, Unit]): LogMedium[E1, A] =
    LogMedium[E1, A](a => log(a).catchAll(_ => fb(a)))
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

  /** Unfailing simple console output */
  def console: LogMedium[Nothing, String] =
    LogMedium[Nothing, String](zio.console.Console.Live.console.putStrLn)

  /** Inhibit log output – useful for unit testing */
  def silent[A]: LogMedium[Nothing, String] =
    LogMedium(_ => ZIO.unit)

}

final case class Tagged[A](level: Level, message: () => A)

object TaggedLogMedium {
  final case class TimestampedMessage[A](level: Level, message: () => A, timestamp: String)

  /** A console with conventional JVM-style logger output */
  def consoleE[A](prefix: Option[String]): LogMedium[Throwable, Tagged[A]] =
    withTagsE(prefix, RawLogMedium.console)

  /**
   * An unfailing console with conventional JVM-style logger output that falls back to
   *  simple console output in the event of any error
   */
  def console[A](prefix: Option[String]): LogMedium[Nothing, Tagged[A]] =
    withTags(prefix, RawLogMedium.console)

  /** Inhibit log output – useful for unit testing */
  def silent[A]: LogMedium[Nothing, Tagged[A]] =
    LogMedium(_ => ZIO.unit)

  ////

  /**
   * Creates a conventional JVM-style logger output over the top of an existing string logger
   *
   * @param prefix an optional application-specific string that can be prepended to each log message
   * @param base the base medium that the rendered string output will be logged to
   * @tparam A type of the messages to be converted to string and logged
   */
  def withTagsE[A](
    prefix: Option[String],
    base: LogMedium[Nothing, String]
  ): LogMedium[Throwable, Tagged[A]] =
    base
      .contramap[TimestampedMessage[A]](asString(prefix))
      .contramapM[Throwable, Tagged[A]](asTimestamped)

  /**
   * Creates an unfailing conventional JVM-style logger output over the top of an existing string logger.
   * In the event of an error (eg generating the timestamp) information, rather than failing, this
   * logger falls back to logging without timestamp.
   *
   * @param prefix an optional application-specific string that can be prepended to each log message
   * @param base the base medium that the rendered string output will be logged to
   * @tparam A type of the messages to be converted to string and logged
   */
  def withTags[A](
    prefix: Option[String],
    base: LogMedium[Nothing, String]
  ): LogMedium[Nothing, Tagged[A]] =
    base
      .contramap[TimestampedMessage[A]](asString(prefix))
      .contramapM[Nothing, Tagged[A]] {
        (t: Tagged[A]) =>
          asTimestamped(t)
            .catchAll(
              _ => ZIO.succeed(TimestampedMessage[A](t.level, t.message, "(timestamp error)"))
            )
      }

  def asTimestamped[A]: Tagged[A] => ZIO[Any, Throwable, TimestampedMessage[A]] =
    (t: Tagged[A]) =>
      ZIO
        .effect(LocalDateTime.now)
        .map(timestampFormat.format)
        .map(TimestampedMessage[A](t.level, t.message, _))

  def asString[A](prefix: Option[String]): TimestampedMessage[A] => String =
    (ts: TimestampedMessage[A]) =>
      "%s %-5s - %s%s".format(
        ts.timestamp,
        ts.level.name,
        prefix.fold("")(s => s"$s: "),
        ts.message()
      )

  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

}
