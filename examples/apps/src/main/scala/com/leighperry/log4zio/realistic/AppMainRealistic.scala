package com.leighperry.log4zio.realistic

import com.leighperry.log4zio.Log
import com.leighperry.log4zio.slf4j.Slf4jLog
import zio.blocking.Blocking
import zio.system.System
import zio.{ App, UIO, ZIO }

final case class ProgramConfig(inputPath: String, outputPath: String)

object AppMain extends App {

  final case class AppEnv(log: Log.Service[String], config: Config.Service, spark: Spark.Service)
    extends Log[String]
    with Config
    with Spark
    with Blocking.Live

  val appName = "realistic-app"
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    for {
      logsvc <- Slf4jLog.logger(Some(appName))
      log = logsvc.log

      pgm = for {
        config <- Config.make
        spark <- Spark.local(appName)
        _ <- Application.execute.provide(AppEnv(log, config.config, spark.spark))
      } yield ()

      exitCode <- pgm.foldM(
        e => log.error(s"Application failed: $e") *> ZIO.succeed(1),
        _ => log.info("Application terminated with no error indication") *> ZIO.succeed(0)
      )
    } yield exitCode
}

////

/** The ADT of error types for the application */
sealed trait AppError
object AppError {
  final case class InvalidConfiguration(error: String) extends AppError
  final case class ExceptionEncountered(message: String) extends AppError

  def exception(e: Throwable): AppError =
    ExceptionEncountered(s"Exception: ${e.getMessage}")

}

////

trait Config {
  def config: Config.Service
}

object Config {
  trait Service {
    def config: UIO[AppConfig]
  }

  def make: ZIO[System, AppError, Config] =
    AppConfig
      .load
      .map(
        cfg =>
          new Config {
            override def config: Service =
              new Service {
                override def config: UIO[AppConfig] =
                  ZIO.succeed(cfg)
              }
          }
      )
}

final case class AppConfig(
  kafka: KafkaConfig
)

object AppConfig {

  def load: ZIO[Any, AppError, AppConfig] =
    ZIO
      .effect(defaults) // dummy implementation
      .mapError(e => AppError.InvalidConfiguration(e.getMessage))

  val defaults: AppConfig =
    AppConfig(
      kafka = KafkaConfig(
        bootstrapServers = KafkaBootstrapServers("localhost:9092"),
        schemaRegistryUrl = KafkaSchemaRegistryUrl("http://localhost:8081"),
        List.empty,
        None
      )
    )

}

case class KafkaConfig(
  bootstrapServers: KafkaBootstrapServers,
  schemaRegistryUrl: KafkaSchemaRegistryUrl,
  properties: List[PropertyValue],
  verbose: Option[Boolean]
)

final case class KafkaBootstrapServers(value: String) extends AnyVal
final case class KafkaSchemaRegistryUrl(value: String) extends AnyVal
final case class PropertyValue(name: String, value: String)

////

final case class SparkSession(name: String) {
  // stubs for the real Spark
  def slowOp(value: String): Unit =
    Thread.sleep(value.length * 100L)

  def version: String =
    "v2.4.4"
}

////

trait Spark {
  def spark: Spark.Service
}

object Spark {
  trait Service {
    def spark: UIO[SparkSession]
  }

  def make(session: => SparkSession): ZIO[Blocking, Throwable, Spark] =
    zio
      .blocking
      .effectBlocking(session)
      .map(
        sparkSession =>
          new Spark {
            override def spark: Service =
              new Service {
                override def spark: UIO[SparkSession] =
                  ZIO.succeed(sparkSession)
              }
          }
      )

  def local(name: String): ZIO[Blocking, Throwable, Spark] =
    make {
      // As a real-world example:
      //    SparkSession.builder().appName(name).master("local").getOrCreate()
      SparkSession(name)
    }

  def cluster(name: String): ZIO[Blocking, Throwable, Spark] =
    make {
      // As a real-world example:
      //    SparkSession.builder().appName(name).enableHiveSupport().getOrCreate()
      SparkSession(name)
    }

}

////

// The core application
object Application {
  val logSomething: ZIO[Log[String] with Config, Nothing, Unit] =
    for {
      cfg <- ZIO.accessM[Config](_.config.config)
      log <- Log.log[String]
      _ <- log.info(s"Executing with parameters ${cfg.kafka} without sparkSession")
    } yield ()

  val runSparkJob: ZIO[Log[String] with Spark with Blocking, Throwable, Unit] =
    for {
      session <- ZIO.accessM[Spark](_.spark.spark)
      result <- zio.blocking.effectBlocking(session.slowOp("SELECT something"))
      log <- Log.log[String]
      _ <- log.info(s"Executed something with spark ${session.version}: $result")
    } yield ()

  val processData: ZIO[Log[String] with Spark with Config, Throwable, Unit] =
    for {
      cfg <- ZIO.accessM[Config](_.config.config)
      spark <- ZIO.accessM[Spark](_.spark.spark)
      log <- Log.log[String]
      _ <- log.info(s"Executing ${cfg.kafka} using ${spark.version}")
    } yield ()

  val execute: ZIO[Log[String] with Spark with Config with Blocking, AppError, Unit] =
    for {
      log <- Log.log[String]
      cfg <- ZIO.accessM[Config](_.config.config)
      _ <- logSomething
      _ <- runSparkJob.mapError(AppError.exception)
      _ <- processData.mapError(AppError.exception)
    } yield ()
}
