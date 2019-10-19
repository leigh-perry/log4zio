package com.leighperry.log4zio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import zio.{ Task, UIO, ZIO }

object Sketch {

  final case class LogAction[A](log: A => UIO[Unit]) {

    def contramap[B](f: B => A): LogAction[B] =
      LogAction[B](b => log(f(b)))

    def contramapM[B](f: B => UIO[A]): LogAction[B] =
      LogAction[B](b => f(b).flatMap(log(_)))

  }

  val logStdout: LogAction[String] =
    LogAction[String](zio.console.Console.Live.console.putStrLn)

  trait Level { val name: String }
  final object Error extends Level { override val name = "ERROR" }
  final object Warn extends Level { override val name = "WARN" }
  final object Info extends Level { override val name = "INFO" }
  final object Debug extends Level { override val name = "DEBUG" }

  final case class LogMsg(message: String, level: Level, timestamp: String)

  // TODO as part of Log
  val prefix = None

  def formatMessage(m: LogMsg): String =
    "%s %-5s - %s%s".format(
      m.timestamp,
      m.level.name,
      prefix.fold("")(s => s"$s: "),
      m.message
    )

  val logMsgStdout: LogAction[LogMsg] =
    logStdout.contramap(formatMessage)

  val logLogMsgStdout: LogAction[(Level, String)] =
    logMsgStdout.contramapM {
      case (level, message) =>
        timestamp.map(LogMsg(message, level, _))
    }

  ////

  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  private val timestamp: UIO[String] =
    Task(LocalDateTime.now)
      .map(timestampFormat.format)
      .catchAll(_ => UIO("(timestamp error)"))

}

object XXX extends zio.App {
  import Sketch._

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    logLogMsgStdout
      .log((Info, "asdfa"))
      .map(_ => 1)
}
