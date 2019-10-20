package com.leighperry.log4zio

import com.leighperry.log4zio.testsupport.TestSupport
import org.scalacheck.Properties

object LogTest extends Properties("LogTest") with TestSupport {

  /*  final case class LogData(
    appName: Option[String],
    logStrings: List[String],
    logFn: (Log[String], String) => ZIO[Any, Nothing, Unit],
    logStep: String => LogStep[String]
  )

  private val genLogData =
    for {
      appName <- Gen.option(genFor[String])
      scount <- Gen.chooseNum[Int](0, 20)
      strings <- Gen.listOfN(scount, genFor[String])
      logAction <- Gen.oneOf(
        ((log: Log[String], s: String) => log.log.error(s)) -> (
          (s: String) => Log.Error[String](() => s)
        ),
        ((log: Log[String], s: String) => log.log.warn(s)) -> (
          (s: String) => Log.Warn[String](() => s)
        ),
        ((log: Log[String], s: String) => log.log.info(s)) -> (
          (s: String) => Log.Info[String](() => s)
        ),
        ((log: Log[String], s: String) => log.log.debug(s)) -> (
          (s: String) => Log.Debug[String](() => s)
        )
      )
    } yield LogData(appName, strings, logAction._1, logAction._2)

  property("logging operations") = forAllZIO(genLogData) {
    ld =>
      for {
        dc <- testLogger(ld.appName)
        (entries, log) = dc
        _ <- ZIO.traverse_(ld.logStrings)(s => ld.logFn(log, s))
        strings <- entries.get
        expected = ld.logStrings.map(s => formatMessage(ld.appName, ld.logStep(s))).reverse
      } yield strings.shouldBe(expected) || true  // TODO remove
  }

  private def testLogger(
    prefix: Option[String]
  ): ZIO[Any, Nothing, (Ref[List[String]], Log[String])] =
    /*for {
      entries <- Ref.make(List.empty[String])
      log <- Log.make[String](formatMessage(prefix, _), b => entries.update(b :: _).unit)
    } yield (entries, log)*/
    ???

  private def formatMessage(prefix: Option[String], l: LogStep[String]) =
    "%-5s - %s%s".format(l.level, prefix.fold("")(s => s"$s: "), l.message())
 */
}
