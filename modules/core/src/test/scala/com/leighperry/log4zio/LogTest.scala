package com.leighperry.log4zio

import com.leighperry.log4zio.Log.{ LogStep, Service }
import com.leighperry.log4zio.testsupport.TestSupport
import org.scalacheck.{ Gen, Properties }
import zio.{ Ref, ZIO }

object LogTest extends Properties("LogTest") with TestSupport {

  final case class LogData(
    appName: Option[String],
    logStrings: List[String],
    logFn: (Log, String) => ZIO[Any, Nothing, Unit],
    logStep: String => LogStep
  )

  private val genLogData =
    for {
      appName <- Gen.option(genFor[String])
      scount <- Gen.chooseNum[Int](0, 20)
      strings <- Gen.listOfN(scount, genFor[String])
      logAction <- Gen.oneOf(
        ((log: Log, s: String) => log.log.error(s)) -> ((s: String) => Log.Error(() => s): LogStep),
        ((log: Log, s: String) => log.log.warn(s)) -> ((s: String) => Log.Warn(() => s): LogStep),
        ((log: Log, s: String) => log.log.info(s)) -> ((s: String) => Log.Info(() => s): LogStep),
        ((log: Log, s: String) => log.log.debug(s)) -> ((s: String) => Log.Debug(() => s): LogStep)
      )
    } yield LogData(appName, strings, logAction._1, logAction._2)

  property("console") = forAllZIO(genLogData) {
    ld =>
      for {
        dc <- testLogger(ld.appName)
        (entries, log) = dc
        _ <- ZIO.traverse_(ld.logStrings)(s => ld.logFn(log, s))
        strings <- entries.get
        expected = ld.logStrings.map(s => formatMessage(ld.appName, ld.logStep(s))).reverse
      } yield strings.shouldBe(expected)
  }

  private def testLogger(prefix: Option[String]): ZIO[Any, Nothing, (Ref[List[String]], Log)] =
    for {
      entries <- Ref.make(List.empty[String])
      log <- ZIO.effectTotal {
        new Log {
          override def log: Service =
            new Service {
              override def log: LogStep => ZIO[Any, Nothing, Unit] =
                l => entries.update(formatMessage(prefix, l) :: _).unit
            }
        }
      }
    } yield (entries, log)

  private def formatMessage(prefix: Option[String], l: LogStep) =
    "%-5s - %s%s".format(l.level, prefix.fold("")(s => s"$s: "), l.message())
}
