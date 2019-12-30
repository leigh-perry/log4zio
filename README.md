# log4zio

![alt text](https://travis-ci.org/leigh-perry/log4zio.svg?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.leigh-perry/log4zio_2.13.svg)](http://search.maven.org/#search|gav|1|g%3A%22com.github.leigh-perry%22%20AND%20a%3A%22log4zio_2.13%22)
![Grain free](https://img.shields.io/badge/grain-free-orange.svg)

```scala
// available for Scala 2.12, 2.13
libraryDependencies += "com.github.leigh-perry" %% "log4zio-core" % "0.2.4"
```

# Introduction

This library targets error-free, composable logger creation.

1. The `log4zio` interface assumes that the user doesn't want to experience logging failures.
Logging is most important under failure conditions, so it is best to log via a 
fallback mechanism rather than fail altogether.
*However, if you prefer to expose logging errors to handle them explicitly, there are error-ful (error-prone?)
versions of the standard loggers available.*

1. The library lets the user create a new logging capability by composing refinements on top of 
a base logging implementation.

## Contravariant composition

Contravariant composition allows you to, if you have a simple-text console logger, create a logger
that includes a timestamp on every message.
Or, it means that you can create a logger that encodes as JSON, writing the JSON data out to a string logger.
Alternatively, you can create a logger that accepts only a specialised `SafeString` data type
for its log messages. Implementing this is easy since the `SafeString` can be converted to `String` for 
the log-writing phase. Or perhaps all your log messages take the form of an integer code, and you 
want to be able to merely call `log.info(101)` to write whatever that means to your log.

This compositional behaviour is that of a *contravariant functor* for the `LogMedium` class.
`Contravariant` functors are characterised by the `contramap` method:

```scala
  def contramap[B](f: B => A): LogMedium[B] = ...
```

It says, if you have a `LogMedium` for type `A`, contramap will give you a `LogMedium` for any `B`, so 
long as you can convert your `B` to an `A`.

# Components

## `LogMedium`

`LogMedium` conveys the basic abstraction of logging: a function from some `A` to `Unit`, 
for example taking a `String` and (effectfully) writing it somewhere.

`LogMedium[A]` is a contravariant functor, so it can be reused to implement another 
`LogMedium[B]` via `contramap`, so long as `B` can be converted to an `A`.

### `TaggedLogMedium`

`TaggedLogMedium` encapsulates the conventional logging pattern, with a logging
level (such as INFO) and timestamp. Addition of the level and timestamp info is also
achieved by `contramap`-ing the additional level and timestamp tags onto a raw logger. 
Eg:
```scala
final case class Tagged[A](level: Level, message: () => A)

object TaggedLogMedium {
  final case class TimestampedMessage[A](level: Level, message: () => A, timestamp: String)

  def console[A](prefix: Option[String]): LogMedium[Tagged[A]] =
    withTags(prefix, RawLogMedium.console)

  def silent[A]: LogMedium[Tagged[A]] =
    LogMedium(_ => ZIO.unit)

  def withTags[A](prefix: Option[String], base: LogMedium[String]): LogMedium[Tagged[A]] =
    base.contramap {
      m: TimestampedMessage[A] =>
        "%s %-5s - %s%s".format(
          m.timestamp,
          m.level.name,
          prefix.fold("")(s => s"$s: "),
          m.message()
        )
    }.contramapM {
      a: Tagged[A] =>
        ZIO
          .effect(LocalDateTime.now)
          .map(timestampFormat.format)
          .catchAll(_ => UIO("(timestamp error)"))
          .map(TimestampedMessage[A](a.level, a.message, _))
    }
}
```

This sample also illustrates the philosophy that logging should not fail.
It is the responsibility of `LogMedium` implementations to implement fallback behaviour. 
In the above example, `catchAll` handles this requirement. 

## `Log.Service`

The ZIO service pattern is implemented in the `Log` class:
```scala
trait Log[A] {
  def log: Log.Service[A]
}

object Log {
  def log[A]: ZIO[Log[A], Nothing, Log.Service[A]] =
    ZIO.access[Log[A]](_.log)

  def stringLog: ZIO[Log[String], Nothing, Log.Service[String]] =
    log[String]

  trait Service[A] {
    def error(s: => A): UIO[Unit]
    def warn(s: => A): UIO[Unit]
    def info(s: => A): UIO[Unit]
    def debug(s: => A): UIO[Unit]
  }
}
```

As most logging is done to `String`-based output media, there is a shortcut `stringLog` accessor
for this case.

# Examples

Example programs can be [found here](./examples/apps/src/main/scala/com/leighperry/log4zio).

## Logging in an application

```scala
object AppMain extends zio.App {

  final case class AppEnv(log: Log.Service[String]) extends Log[String]

  val appName = "logging-app"
  
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    for {
      logsvc <- Log.console[String](Some(appName))
      log = logsvc.log
      pgm = Application.execute.provide(AppEnv(log))
      :
    } yield 0

  val doSomething: ZIO[Log[String], Nothing, Unit] =
    for {
      log <- Log.stringLog
      _ <- log.info(s"Executing something")
      _ <- log.info(s"Finished executing something")
    } yield ()

  val execute: ZIO[Log[String], Nothing, Unit] =
    for {
      log <- Log.stringLog
      _ <- log.info(s"Starting app")
      _ <- doSomething
      _ <- log.info(s"Finished app")
    } yield ()
}
```

A more realistic sample application using SLF4J logging is [found here](./examples/apps/src/main/scala/com/leighperry/log4zio/realistic/AppMain.scala).

## Logging Int messages

You probably won't do this using `Int` logging, but custom error types can be created by `contramap`-ing a string-based logger:
```scala
object AppMain extends zio.App {

  def intLogger: UIO[Log[Int]] =
    Log.make[Int](intRendered(RawLogMedium.console))

  def intRendered(base: LogMedium[String]): LogMedium[Tagged[Int]] =
    base.contramap {
      m: Tagged[Int] =>
        val n: Int = m.message()
        "%-5s - %d:%s".format(m.level.name, n, "x" * n)
    }

  final case class AppEnv(log: Log.Service[Int]) extends Log[Int]

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    for {
      logsvc <- intLogger
      log = logsvc.log
      pgm = execute.provide(AppEnv(log))
      :
    } yield 0

  val doSomething: ZIO[Log[Int], Nothing, Unit] =
    for {
      log <- Log.log[Int]
      _ <- log.info(1)
      _ <- log.info(2)
    } yield ()

  val execute: ZIO[Log[Int], Nothing, Unit] =
    for {
      log <- Log.log[Int]
      _ <- log.info(3)
      _ <- doSomething
      _ <- log.info(4)
    } yield ()
}
```


# Release

```bash
VERS=0.2.5
git tag -a v${VERS} -m "v${VERS}"
git push origin v${VERS}
```
