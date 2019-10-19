package com.leighperry.log4zio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import zio.{ Task, UIO, ZIO }

final case class LogWriter[A](log: A => UIO[Unit]) {

  def contramap[B](f: B => A): LogWriter[B] =
    LogWriter[B](b => log(f(b)))

  def contramapM[B](f: B => UIO[A]): LogWriter[B] =
    LogWriter[B](b => f(b).flatMap(log))

}

object RawConsole {
  val log: LogWriter[String] =
    LogWriter[String](zio.console.Console.Live.console.putStrLn)
}

object TaggedLogger {

  trait Level { val name: String }
  object Error extends Level { override val name = "ERROR" }
  object Warn extends Level { override val name = "WARN" }
  object Info extends Level { override val name = "INFO" }
  object Debug extends Level { override val name = "DEBUG" }

  final case class TaggedMessage(message: String, level: Level, timestamp: String)

  // TODO as part of Log
  val prefix = None

  def formatMessage(m: TaggedMessage): String =
    "%s %-5s - %s%s".format(
      m.timestamp,
      m.level.name,
      prefix.fold("")(s => s"$s: "),
      m.message
    )

  val rawLog: LogWriter[TaggedMessage] =
    RawConsole.log.contramap(formatMessage)

  val log: LogWriter[(Level, String)] =
    rawLog.contramapM {
      case (level, message) =>
        timestamp.map(TaggedMessage(message, level, _))
    }

  //// shortcuts

  def error(s: String): UIO[Unit] = log.log(Error -> s)
  def warn(s: String): UIO[Unit] = log.log(Warn -> s)
  def info(s: String): UIO[Unit] = log.log(Info -> s)
  def debug(s: String): UIO[Unit] = log.log(Debug -> s)

  ////

  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  private lazy val timestamp: UIO[String] =
    Task(LocalDateTime.now)
      .map(timestampFormat.format)
      .catchAll(_ => UIO("(timestamp error)"))
}

object XXX extends zio.App {

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    (TaggedLogger.info("someinfo") *> TaggedLogger.error("someerror"))
      .map(_ => 1)
}
