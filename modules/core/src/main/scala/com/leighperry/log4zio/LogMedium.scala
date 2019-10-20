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

final case class TaggedMessage[A](message: A, level: Level, timestamp: String)

//// Log medium writer implementations

object RawLogMedium {

  def console[A]: LogMedium[A] =
    LogMedium[A](a => zio.console.Console.Live.console.putStrLn(a.toString)) // TODO toString?

}

object TaggedStringLogMedium {
  def silent: LogMedium[(Level, String)] =
    LogMedium(_ => ZIO.unit)

  def console(prefix: Option[String]): LogMedium[(Level, String)] =
    RawLogMedium
      .console
      .contramap {
        m: TaggedMessage[String] =>
          "%s %-5s - %s%s".format(
            m.timestamp,
            m.level.name,
            prefix.fold("")(s => s"$s: "),
            m.message
          )
      }
      .contramapM {
        case (level, message) =>
          ZIO
            .effect(LocalDateTime.now)
            .map(timestampFormat.format)
            .catchAll(_ => UIO("(timestamp error)"))
            .map(TaggedMessage(message, level, _))
      }

  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
}

/*
final class TaggedLog private (logMedium: LogMedium[(Level, String)]) {

  // TODO move to service
  def error(s: String): UIO[Unit] =
    logMedium.log(Level.Error -> s)

  def warn(s: String): UIO[Unit] =
    logMedium.log(Level.Warn -> s)

  def info(s: String): UIO[Unit] =
    logMedium.log(Level.Info -> s)

  def debug(s: String): UIO[Unit] =
    logMedium.log(Level.Debug -> s)

}

object TaggedLog {
  def apply(logMedium: LogMedium[(Level, String)]): TaggedLog =
    new TaggedLog(logMedium)
}

object TestConsoleLog extends zio.App {

  val logger = TaggedLog(TaggedStringLogMedium.console(Some("an-app")))

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    (logger.info("someinfo") *> logger.error("someerror"))
      .map(_ => 1)
}
*/
