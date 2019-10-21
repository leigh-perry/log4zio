package com.leighperry.log4zio.loggingonly

import com.leighperry.log4zio.Log
import zio.ZIO

object AppMain extends zio.App {

  final case class AppEnv(log: Log.Service[String]) extends Log[String]

  val appName = "logging-app"
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    for {
      logsvc <- Log.console[String](Some(appName))
      log = logsvc.log

      pgm = for {
        _ <- Application.execute.provide(AppEnv(log))
      } yield ()

      exitCode <- pgm.foldM(
        e => log.error(s"Application failed: $e") *> ZIO.succeed(1),
        _ => log.info("Application terminated with no error indication") *> ZIO.succeed(0)
      )
    } yield exitCode
}

// The core application
object Application {
  val doSomething: ZIO[Log[String], Nothing, Unit] =
    for {
      log <- Log.ofString
      _ <- log.info(s"Executing something")
      _ <- log.info(s"Finished executing something")
    } yield ()

  val execute: ZIO[Log[String], Nothing, Unit] =
    for {
      log <- Log.ofString
      _ <- log.info(s"Starting app")
      _ <- doSomething
      _ <- log.info(s"Finished app")
    } yield ()
}
