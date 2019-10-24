package com.leighperry.log4zio.loggingonly

import com.leighperry.log4zio.Log
import zio.ZIO

object AppMain extends zio.App {

  final case class AppEnv(log: Log.Service[Nothing, String]) extends Log[Nothing, String]

  val appName = "logging-app"

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    for {
      logsvc <- Log.safeConsole[String](Some(appName))
      log = logsvc.log

      pgm = Application.execute.provide(AppEnv(log))

      exitCode <- pgm.foldM(
        e => log.error(s"Application failed: $e") *> ZIO.succeed(1),
        _ => log.info("Application terminated with no error indication") *> ZIO.succeed(0)
      )
    } yield exitCode
}

// The core application
object Application {
  val doSomething: ZIO[Log[Nothing, String], Nothing, Unit] =
    for {
      log <- Log.stringLog
      _ <- log.info(s"Executing something")
      _ <- log.info(s"Finished executing something")
    } yield ()

  val execute: ZIO[Log[Nothing, String], Nothing, Unit] =
    for {
      log <- Log.stringLog
      _ <- log.info(s"Starting app")
      _ <- doSomething
      _ <- log.info(s"Finished app")
    } yield ()
}
