package com.leighperry.log4zio.slf4j

import com.leighperry.log4zio.Log
import zio.{ Task, UIO, ZIO }

object Slf4j {

//  /**
//   * This implementation meets the Log.Service requirement logging does not emit failures, so it
//   * implements fallback to console.
//   */
//  def slf4j[A](prefix: Option[A], fRenderA: A => String): ZIO[Any, Nothing, Log[A, String]] =
//    ZIO.effect {
//      org.slf4j.LoggerFactory.getLogger(getClass)
//    }.map {
//      slfLogger =>
//        new Log[String, String] {
//          override def log: Log.Service[String, String] =
//            new Log.Service[A, String] {
//              override def log(ls: Log.LogStep[A]): ZIO[Any, Nothing, Unit] = {
//                def write(message: () => A, fOutput: String => Unit): Task[Unit] =
//                  ZIO.effect(fOutput(fRenderA(message())))
//
//                val result: Task[Unit] =
//                  ls match {
//                    case Log.Error(message) =>
//                      write(message, slfLogger.error)
//                    case Log.Warn(message) =>
//                      write(message, slfLogger.warn)
//                    case Log.Info(message) =>
//                      write(message, slfLogger.info)
//                    case Log.Debug(message) =>
//                      write(message, slfLogger.debug)
//                  }
//
//                result.catchAll(_ => Log.console(prefix).flatMap(_.log.log(ls))) // fallback on write failure
//              }
//
//              // TODO separate concerns log vs render/output
//              override def render(ls: Log.LogStep[A]): String =
//                ???
//
//              override def output(b: String): UIO[Unit] =
//                ???
//
//            }
//        }
//    }.catchAll(_ => Log.console(prefix)) // fallback on creation failure

}
