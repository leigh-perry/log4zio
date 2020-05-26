package com.leighperry.log4zio.loggingonly

import com.leighperry.log4zio.Log
import com.leighperry.log4zio.Log.SafeLog
import zio.{ExitCode, IO, ZIO}

object AppMain extends zio.App {

  val appName = "logging-app"

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    for {
      log <- Log.console[String](Some(appName))

      exitCode <- new Application(log).execute *>
        log.info("Application terminated with no error indication") *>
        ZIO.succeed(ExitCode.success)

    } yield exitCode
}

// The core application
class Application(log: SafeLog[String]) {
  val doSomething: IO[Nothing, Unit] =
    for {
      _ <- log.info(s"Executing something")
      _ <- log.info(s"Finished executing something")
    } yield ()

  val execute: IO[Nothing, Unit] =
    for {
      _ <- log.info(s"Starting app")
      _ <- doSomething
      _ <- log.info(s"Finished app")
    } yield ()
}
