package com.leighperry.log4zio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import zio.{ UIO, ZIO }

final case class LogWriter[A](log: A => UIO[Unit]) {

  def contramap[B](f: B => A): LogWriter[B] =
    LogWriter[B](b => log(f(b)))

  def contramapM[B](f: B => UIO[A]): LogWriter[B] =
    LogWriter[B](b => f(b).flatMap(log))

}

////

trait Level { val name: String }
object Error extends Level { override val name = "ERROR" }
object Warn extends Level { override val name = "WARN" }
object Info extends Level { override val name = "INFO" }
object Debug extends Level { override val name = "DEBUG" }

final case class TaggedMessage[A](message: A, level: Level, timestamp: String)

//// output implementations

object RawLogger {
  def console[A]: LogWriter[A] =
    LogWriter[A](a => zio.console.Console.Live.console.putStrLn(a.toString)) // TODO toString?
}

object TaggedStringLogWriter {

  def console(prefix: Option[String]): LogWriter[(Level, String)] =
    RawLogger
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

final class Logger private (logWriter: LogWriter[(Level, String)]) {

  def error(prefix: Option[String], s: String): UIO[Unit] =
    logWriter.log(Error -> s)

  def warn(prefix: Option[String], s: String): UIO[Unit] =
    logWriter.log(Warn -> s)

  def info(prefix: Option[String], s: String): UIO[Unit] =
    logWriter.log(Info -> s)

  def debug(prefix: Option[String], s: String): UIO[Unit] =
    logWriter.log(Debug -> s)

}

object Logger {
  def apply(logWriter: LogWriter[(Level, String)]): Logger =
    new Logger(logWriter)
}

object XXX extends zio.App {

  val logger = Logger(TaggedStringLogWriter.console(Some("an-app")))

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    (logger.info(None, "someinfo") *> logger.error(None, "someerror"))
      .map(_ => 1)
}
