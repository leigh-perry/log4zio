package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.Log
import zio.{Task, ZIO}

object Slf4j {

  import com.leighperry.log4zio.Log._

  /**
   * This implementation meets the Log.Service requirement logging does not emit failures, so it
   * implements fallback to console.
   */
  def slf4j(prefix: Option[String]): ZIO[Any, Nothing, Log] =
    ZIO.effect {
      org.slf4j.LoggerFactory.getLogger(getClass)
    }.map {
      slfLogger =>
        new Log {
          override def log: Log.Service =
            new Log.Service {
              override def log: LogStep => ZIO[Any, Nothing, Unit] =
                entry => {
                  def write(message: () => String, f: String => Unit): Task[Unit] =
                    ZIO.effect(f(message()))

                  val result: Task[Unit] =
                    entry match {
                      case Error(message) =>
                        write(message, slfLogger.error)
                      case Warn(message) =>
                        write(message, slfLogger.warn)
                      case Info(message) =>
                        write(message, slfLogger.info)
                      case Debug(message) =>
                        write(message, slfLogger.debug)
                    }

                  result.catchAll(_ => console(prefix).flatMap(_.log.log(entry))) // fallback on failure
                }
            }
        }
    }.catchAll(_ => console(prefix)) // fallback on failure

}
