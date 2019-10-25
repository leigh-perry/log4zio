package com.leighperry.log4zio.loggingonly

import com.leighperry.log4zio.Log
import com.leighperry.log4zio.Log.SafeLog
import zio.ZIO

object AppMain extends zio.App {

  final case class AppEnv(log: Log.Service[Nothing, String]) extends SafeLog[String]

  val appName = "logging-app"

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    for {
      logsvc <- Log.console[String](Some(appName))
      log = logsvc.log

      pgm = Application.execute.provide(AppEnv(log))

      exitCode <- pgm *> log.info("Application terminated with no error indication") *> ZIO.succeed(
        0
      )
    } yield exitCode
}

// The core application
object Application {
  val doSomething: ZIO[SafeLog[String], Nothing, Unit] =
    for {
      log <- Log.stringLog
      _ <- log.info(s"Executing something")
      _ <- log.info(s"Finished executing something")
    } yield ()

  val execute: ZIO[SafeLog[String], Nothing, Unit] =
    for {
      log <- Log.stringLog
      _ <- log.info(s"Starting app")
      _ <- doSomething
      _ <- log.info(s"Finished app")
    } yield ()
}
