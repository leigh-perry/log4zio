package com.leighperry.log4zio.nonstring

import com.leighperry.log4zio.Log.SafeLog
import com.leighperry.log4zio.{Log, LogMedium, RawLogMedium, Tagged}
import zio.{UIO, ZIO}

object AppMain extends zio.App {

  def intLogger: UIO[SafeLog[Int]] =
    Log.make[Nothing, Int](intRendered(RawLogMedium.console))

  def intRendered(base: LogMedium[Nothing, String]): LogMedium[Nothing, Tagged[Int]] =
    base.contramap {
      m: Tagged[Int] =>
        val n: Int = m.message()
        "%-5s - %d:%s".format(m.level.name, n, "x" * n)
    }

  final case class AppEnv(log: Log.Service[Nothing, Int]) extends SafeLog[Int]

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    for {
      logsvc <- intLogger
      log = logsvc.log

      pgm = Application.execute.provide(AppEnv(log))

      exitCode <- pgm *> log.info(10) *> ZIO.succeed(0)
    } yield exitCode
}

// The core application
object Application {
  val doSomething: ZIO[SafeLog[Int], Nothing, Unit] =
    for {
      log <- Log.log[Nothing, Int]
      _ <- log.info(1)
      _ <- log.info(2)
    } yield ()

  val execute: ZIO[SafeLog[Int], Nothing, Unit] =
    for {
      log <- Log.log[Nothing, Int]
      _ <- log.info(3)
      _ <- doSomething
      _ <- log.info(4)
    } yield ()
}
