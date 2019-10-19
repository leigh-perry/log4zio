package com.leighperry.log4zio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import zio.{ Task, UIO, ZIO }

final case class Logger[A](log: A => UIO[Unit]) {

  def contramap[B](f: B => A): Logger[B] =
    Logger[B](b => log(f(b)))

  def contramapM[B](f: B => UIO[A]): Logger[B] =
    Logger[B](b => f(b).flatMap(log(_)))

}

object RawConsole {
  val logger: Logger[String] =
    Logger[String](zio.console.Console.Live.console.putStrLn)
}

trait Level { val name: String }
object Error extends Level { override val name = "ERROR" }
object Warn extends Level { override val name = "WARN" }
object Info extends Level { override val name = "INFO" }
object Debug extends Level { override val name = "DEBUG" }

final case class TaggedMessage(message: String, level: Level, timestamp: String)

object TaggedMessage {
  // TODO as part of Log
  val prefix = None

  def formatMessage(m: TaggedMessage): String =
    "%s %-5s - %s%s".format(
      m.timestamp,
      m.level.name,
      prefix.fold("")(s => s"$s: "),
      m.message
    )

  val rawLogger: Logger[TaggedMessage] =
    RawConsole.logger.contramap(formatMessage)

  val logger: Logger[(Level, String)] =
    rawLogger.contramapM {
      case (level, message) =>
        timestamp.map(TaggedMessage(message, level, _))
    }

  //// shortcuts

  def error(s: String): UIO[Unit] = logger.log(Error, s)
  def warn(s: String): UIO[Unit] = logger.log(Warn, s)
  def info(s: String): UIO[Unit] = logger.log(Info, s)
  def debug(s: String): UIO[Unit] = logger.log(Debug, s)

  ////

  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  private val timestamp: UIO[String] =
    Task(LocalDateTime.now)
      .map(timestampFormat.format)
      .catchAll(_ => UIO("(timestamp error)"))
}

object XXX extends zio.App {

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    (TaggedMessage.info("someinfo") *> TaggedMessage.error("someerror"))
      .map(_ => 1)
}
