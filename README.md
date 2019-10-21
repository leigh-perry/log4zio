# Log4zio

# Introduction

This logging library targets error-free composability.

1. This interface assumes that the user doesn't want to experience logging failures.
Logging is most important under failure conditions, so it is best to log via a 
fallback mechanism rather than fail altogether. 

1. The library lets the user create a new logging capability by composing refinements on top of 
a base logging implementation.

## Contravariant logging

For instance, if you have a simple logger to the console, you can create a logger
that includes a timestamp on every message.
Or you can create a logger that encodes as JSON, writing the JSON data out to a string logger.
Alternatively, you can create a logger that accepts only a specialised `SafeString` data type
for its log messages – this is easy since the `SafeString` can be converted to `String` for 
the final log-writing phase.

This compositional behaviour is that of a *contravariant functor*.
`Contravariant` functors are characterised by the `contramap` method:

```scala
  def contramap[B](f: B => A): LogMedium[B] = ...
```

# Components

## `LogMedium`

`LogMedium` conveys the basic abstraction of logging: a function from some `A` to `Unit`, 
for example taking a `String` and (effectfully) writing it somewhere.

`LogMedium[A]` is a contravariant functor, so it can be reused to implement another 
`LogMedium[B]` via `contramap`, so long as `B` can be converted to an `A`.

### `TaggedStringLogMedium`

`TaggedStringLogMedium` encapsulates the conventional logging pattern, with a logging
level (such as INFO) and timestamp. Addition of the level and timestamp info is also
achieved by `contramap`-ing the additional level and timestamp tags onto a raw logger. 
Eg:
```scala
final case class Tagged[A](level: Level, message: () => A)

object TaggedStringLogMedium {
  final case class TimestampedMessage[A](message: () => A, level: Level, timestamp: String)

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
          .map(TimestampedMessage[A](a.message, a.level, _))
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

TODO... write this

# Release

```bash
VERS=0.1.1
git tag -a v${VERS} -m "v${VERS}"
git push origin v${VERS}
```