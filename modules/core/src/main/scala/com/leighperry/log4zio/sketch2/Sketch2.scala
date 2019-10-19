package com.leighperry.log4zio.sketch2

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.leighperry.log4zio.sketch2.ConsoleLog.stringLogger
import zio.{ Task, UIO, ZIO }

trait Level { val name: String }
object Error extends Level { override val name = "ERROR" }
object Warn extends Level { override val name = "WARN" }
object Info extends Level { override val name = "INFO" }
object Debug extends Level { override val name = "DEBUG" }

final case class Step[A](message: () => A, level: Level) {
  self =>

  def map[B](f: A => B): Step[B] =
    new Step[B](() => f(self.message()), self.level)

  def as[B](b: B): Step[B] =
    map(_ => b)
}

////

final case class LogWriter[A](log: A => UIO[Unit]) {

  def contramap[B](f: B => A): LogWriter[B] =
    LogWriter[B](b => log(f(b)))

  def contramapM[B](f: B => UIO[A]): LogWriter[B] =
    LogWriter[B](b => f(b).flatMap(log))

}

object ConsoleLog {
  val stringLogger: LogWriter[Step[String]] =
    LogWriter[Step[String]] {
      ss =>
        zio.console.Console.Live.console.putStrLn(ss.message())
    }

  def levelLogger(prefix: Option[String]): LogWriter[Step[String]] =
    stringLogger.contramap {
      ss =>
        ss.as(
          "%s %-5s - %s%s".format(
            ss.level.name,
            prefix.fold("")(s => s"$s: "),
            ss.message
          )
        )
    }
}

////

object TimestampedConsoleLog {

  // TODO push [A] upwards
  // TODO lazy message
  final case class TimestampedMessage[A](message: () => String, timestamp: String)

//  def formatMessage(prefix: Option[String])(m: TimestampedMessage[String]): String =
//    "%s %-5s - %s%s".format(
//      m.timestamp,
//      m.step.level.name,
//      prefix.fold("")(s => s"$s: "),
//      m.step.message()
//    )
//
//  def rawLogger(prefix: Option[String]): LogWriter[TimestampedMessage] =
//    ConsoleLog.stringLogger.contramap(formatMessage(prefix))

  def timestampedLogger(prefix: Option[String]): LogWriter[Step[TimestampedMessage[String]]] =
    stringLogger.contramap {
      ss =>
        ss.as(
          "%s %-5s - %s%s".format(
            ss.message().timestamp,
            ss.level.name,
            prefix.fold("")(s => s"$s: "),
            ss.message().message()
          )
        )
    }

  //// shortcuts

  def error(s: String): UIO[Unit] =
    timestampedLogger(None).log(Step(() => TimestampedMessage(() => s, "timestamp"), Error))
  def warn(s: String): UIO[Unit] =
    timestampedLogger(None).log(Step(() => TimestampedMessage(() => s, "timestamp"), Warn))
  def info(s: String): UIO[Unit] =
    timestampedLogger(None).log(Step(() => TimestampedMessage(() => s, "timestamp"), Info))
  def debug(s: String): UIO[Unit] =
    timestampedLogger(None).log(Step(() => TimestampedMessage(() => s, "timestamp"), Debug))

  ////

  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  private lazy val timestamp: UIO[String] =
    Task(LocalDateTime.now)
      .map(timestampFormat.format)
      .catchAll(_ => UIO("(timestamp error)"))
}

object XXX extends zio.App {

//  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
//    (TimestampedConsoleLog.info("someinfo") *> TimestampedConsoleLog.error("someerror"))
//      .map(_ => 1)
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
  ???
}
