package com.leighperry.log4zio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.leighperry.log4zio.TaggedLogger.{ Debug, Error, Info, Warn }
import zio.{ UIO, ZIO }

final case class LogWriter[A](log: A => UIO[Unit]) {

  def contramap[B](f: B => A): LogWriter[B] =
    LogWriter[B](b => log(f(b)))

  def contramapM[B](f: B => UIO[A]): LogWriter[B] =
    LogWriter[B](b => f(b).flatMap(log))

}

object RawLogger {
  def console[A]: LogWriter[A] =
    LogWriter[A](a => zio.console.Console.Live.console.putStrLn(a.toString)) // TODO toString?
}

object TaggedLogger {

  def console[A](prefix: Option[String]): LogWriter[(Level, String)] =
    taggedMessageWriter(prefix).contramapM {
      case (level, message) =>
        ZIO
          .effect(LocalDateTime.now)
          .map(timestampFormat.format)
          .catchAll(_ => UIO("(timestamp error)"))
          .map(TaggedMessage(message, level, _))
    }

  trait Level { val name: String }
  final object Error extends Level { override val name = "ERROR" }
  final object Warn extends Level { override val name = "WARN" }
  final object Info extends Level { override val name = "INFO" }
  final object Debug extends Level { override val name = "DEBUG" }

  // TODO message: () => String
  private final case class TaggedMessage[A](message: A, level: Level, timestamp: String)

  private def taggedMessageWriter[A](prefix: Option[String]): LogWriter[TaggedMessage[A]] =
    RawLogger.console.contramap {
      m =>
        "%s %-5s - %s%s".format(
          m.timestamp,
          m.level.name,
          prefix.fold("")(s => s"$s: "),
          m.message
        )
    }

  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
}

////

final case class TaggedLogger(prefix: Option[String]) {

  // TODO inject
  private val logWriter = TaggedLogger.console[String](prefix)

  def error(prefix: Option[String], s: String): UIO[Unit] =
    logWriter.log(Error -> s)

  def warn(prefix: Option[String], s: String): UIO[Unit] =
    logWriter.log(Warn -> s)

  def info(prefix: Option[String], s: String): UIO[Unit] =
    logWriter.log(Info -> s)

  def debug(prefix: Option[String], s: String): UIO[Unit] =
    logWriter.log(Debug -> s)

}

object XXX extends zio.App {

  val logger = TaggedLogger(Some("HEY"))

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    (logger.info(None, "someinfo") *> logger.error(None, "someerror"))
      .map(_ => 1)
}
