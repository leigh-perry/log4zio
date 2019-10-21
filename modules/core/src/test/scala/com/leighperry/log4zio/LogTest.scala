package com.leighperry.log4zio

import com.leighperry.log4zio.testsupport.TestSupport
import org.scalacheck.{ Gen, Properties }
import zio.{ Ref, ZIO }

object LogTest extends Properties("LogTest") with TestSupport {

  final case class LogData(
    appName: Option[String],
    logStrings: List[String],
    logFn: (Log[String], String) => ZIO[Any, Nothing, Unit],
    level: Level
  )

  private val genLogData =
    for {
      appName <- Gen.option(genFor[String])
      scount <- Gen.chooseNum[Int](0, 20)
      strings <- Gen.listOfN(scount, genFor[String])
      logAction <- Gen.oneOf(
        ((log: Log[String], s: String) => log.log.error(s), Level.Error),
        ((log: Log[String], s: String) => log.log.warn(s), Level.Warn),
        ((log: Log[String], s: String) => log.log.info(s), Level.Info),
        ((log: Log[String], s: String) => log.log.debug(s), Level.Debug)
      )
    } yield LogData(appName, strings, logAction._1, logAction._2)

  property("logging operations") = forAllZIO(genLogData) {
    ld =>
      for {
        dc <- testLogger(ld.appName)
        (entries, log) = dc
        _ <- ZIO.traverse_(ld.logStrings)(s => ld.logFn(log, s))
        strings <- entries.get
        expected = ld
          .logStrings
          .map(a => formatMessage(ld.appName, Tagged[String](ld.level, () => a)))
          .reverse
      } yield strings.shouldBe(expected)
  }

  private def testLogger(
    prefix: Option[String]
  ): ZIO[Any, Nothing, (Ref[List[String]], Log[String])] =
    for {
      entries <- Ref.make(List.empty[String])
      testMedium = LogMedium[String](a => entries.update(a :: _).unit)
      taggedTestMedium = testMedium.contramap(formatMessage(prefix, (_: Tagged[String])))
      log <- Log.make[String](taggedTestMedium)
    } yield (entries, log)

  private def formatMessage(prefix: Option[String], m: Tagged[String]) =
    "[%s][%s][%s]".format(m.level.name, prefix.fold("")(s => s"$s: "), m.message())
}
