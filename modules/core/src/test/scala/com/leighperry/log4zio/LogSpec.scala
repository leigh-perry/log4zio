package com.leighperry.log4zio

import com.leighperry.log4zio.Log.SafeLog
import zio.random.Random
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{IO, Ref}

object LogSpec extends DefaultRunnableSpec {
  def spec: Spec[TestEnvironment, TestFailure[Nothing], TestSuccess] =
    suite("Logging")(
      testM("Log data formatting") {
        checkM(LogData.gen) {
          ld =>
            val formatted: IO[Nothing, List[String]] =
              for {
                dc <- testLogger(ld.appName)
                (entries, log) = dc
                _ <- IO.foreach_(ld.logStrings)(s => ld.logFn(log, s)) // traverse_ :-(
                strings <- entries.get
              } yield strings

            val expected =
              ld.logStrings
                .map(a => formatMessage(ld.appName, Tagged[String](ld.level, () => a)))
                .reverse

            assertM(formatted)(equalTo(expected))
        }
      }
    )

  final case class LogData(
    appName: Option[String],
    logStrings: List[String],
    logFn: (SafeLog[String], String) => IO[Nothing, Unit],
    level: Level
  )
  object LogData {
    val gen: Gen[Random with Sized, LogData] =
      for {
        appName <- Gen.option(Gen.anyString)
        scount <- Gen.int(0, 20)
        strings <- Gen.listOfN(scount)(Gen.anyString)
        logAction <- Gen.oneOf(
          Gen.const(((log: SafeLog[String], s: String) => log.error(s), Level.Error)),
          Gen.const(((log: SafeLog[String], s: String) => log.warn(s), Level.Warn)),
          Gen.const(((log: SafeLog[String], s: String) => log.info(s), Level.Info)),
          Gen.const(((log: SafeLog[String], s: String) => log.debug(s), Level.Debug))
        )
      } yield LogData(appName, strings, logAction._1, logAction._2)
  }

  private def testLogger(prefix: Option[String]): IO[Nothing, (Ref[List[String]], SafeLog[String])] =
    for {
      entries <- Ref.make(List.empty[String])
      testMedium = LogMedium[Nothing, String](a => entries.update(a :: _).unit)
      taggedTestMedium = testMedium.contramap(formatMessage(prefix, (_: Tagged[String])))
      log <- Log.make[Nothing, String](taggedTestMedium)
    } yield (entries, log)

  private def formatMessage(prefix: Option[String], m: Tagged[String]) =
    "[%s][%s][%s]".format(m.level.name, prefix.fold("")(s => s"$s "), m.message())

}
