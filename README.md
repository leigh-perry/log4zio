# Log4zio - application configuration through induction

   * [Contents](#log4zio---application-configuration-through-induction)
      * [Naming](#naming)
      * [Supported types](#supported-types)
         * [Primitives](#primitives)
         * [List](#list)
         * [Option](#option)
         * [Either](#either)
      * [Supporting new primitive types](#supporting-new-primitive-types)
         * [Supporting newtypes](#supporting-newtypes)
      * [Supporting other effects](#supporting-other-effects)
      * [Environment options](#environment-options)
      * [Error reporting](#error-reporting)

```scala
// available for Scala 2.12, 2.13
libraryDependencies += "com.github.leigh-perry" %% "log4zio-core" % "0.4.2"
```

Configuration is via a configuration library that inductively derives the configuration for known
types. It is able to decode nested classes of arbitrary complexity from key-value pairs, typically
environment variables or system properties.

For example, if your app has the following configuration:

```scala
  final case class AppConfig(
    appName: String,
    endpoint: Endpoint,
    role: Option[AppRole],
    intermediates: List[TwoEndpoints],
  )

  final case class Endpoint(host: String, port: Int)
  final case class TwoEndpoints(ep1: Endpoint, ep2: Endpoint)

  // A String newtype
  final case class AppRole(value: String) extends AnyVal
```

then loading it via:
```scala
      Configured[IO, AppConfig]
        .value("MYAPP")
        .run(Environment.fromEnvVars)
```

with the environment variables:
```bash
export MYAPP_APP_NAME=someAppName
export MYAPP_ENDPOINT_HOST=12.23.34.45
export MYAPP_ENDPOINT_PORT=6789
export MYAPP_ROLE_OPT=somerole
export MYAPP_INTERMEDIATE_COUNT=2
export MYAPP_INTERMEDIATE_0_EP1_HOST=11.11.11.11
export MYAPP_INTERMEDIATE_0_EP1_PORT=6790
export MYAPP_INTERMEDIATE_0_EP2_HOST=22.22.22.22
export MYAPP_INTERMEDIATE_0_EP2_PORT=6791
export MYAPP_INTERMEDIATE_1_EP1_HOST=33.33.33.33
export MYAPP_INTERMEDIATE_1_EP1_PORT=6792
export MYAPP_INTERMEDIATE_1_EP2_HOST=44.44.44.44
export MYAPP_INTERMEDIATE_1_EP2_PORT=6793
```

would yield the following instance of `AppConfig`:
```scala
    AppConfig(
      "someAppName",
      Endpoint("12.23.34.45", 6789),
      Some(AppRole("somerole"),
      List(
        TwoEndpoints(
          Endpoint("11.11.11.11", 6790),
          Endpoint("22.22.22.22", 6791)
        ),
        TwoEndpoints(
          Endpoint("33.33.33.33", 6792),
          Endpoint("44.44.44.44", 6793)
        )
      )
    )
```

Note: to support this, you need to also tell the library how to decode each component data item by
defining an implicit instance, usually in the companion object of each type as follows:
```scala
  object AppConfig {
    implicit def configured[F[_] : Monad]: Configured[F, AppConfig] = (
      Configured[F, String].withSuffix("APP_NAME"),
      Configured[F, Endpoint].withSuffix("ENDPOINT"),
      Configured[F, Option[AppRole]].withSuffix("ROLE"),
      Configured[F, List[TwoEndpoints]].withSuffix("INTERMEDIATE"),
    ).mapN(AppConfig.apply)
  }

object Endpoint {
  implicit def configuredf[F[_]](implicit F: Applicative[F]): Configured[F, Endpoint] = (
    Configured[F, String].withSuffix("HOST"),
    Configured[F, Int].withSuffix("PORT")
  ).mapN(Endpoint.apply)
}

object TwoEndpoints {
  implicit def configuredf[F[_]](implicit F: Applicative[F]): Configured[F, TwoEndpoints] = (
    Configured[F, Endpoint].withSuffix("EP1"),
    Configured[F, Endpoint].withSuffix("EP2")
  ).mapN(TwoEndpoints.apply)
}

object AppRole {
  implicit def conversion: Conversion[AppRole] =
    Conversion[String].map(AppRole.apply)
}
```

## Naming

Each data item is retrieved from a key-value pair (typically an environment variable, with the key being the environment variable name).
Key naming reflects the structure of the configuration case class. In the example above, configuration was loaded via
```scala
      Configured[IO, AppConfig]
        .value("MYAPP")
```

so all keys will begin with `MYAPP_`.
The `Configured` typeclass instance for `AppConfig`, defined in `AppConfig`'s companion object loads the appName field using
```scala
      Configured[F, String].withSuffix("APP_NAME")
```
so the key `MYAPP_APP_NAME` is used to load the value `someAppName`.
The key name is formed by concatenating the overall name `MYAPP` with the suffix fragment `APP_NAME`.
When assembling a composite key name, the fragments are separated by `_`, yielding `MYAPP_APP_NAME`.

By virtue of the inductive derivation of `Configured` typeclass instances for each configuration element,
configuration classes can contain primitive types, nested case classes, and other Scala constructs like `List`, `Option`, and `Either`.

## Supported types

### Primitives

### List

List configuration consists of a count value plus a value for every element of the list.
The count field has suffix fragment `COUNT`, and each field has suffix fragment specifying the index within the list, starting from 0.

In the example above,
```bash
export MYAPP_INTERMEDIATE_COUNT=2
export MYAPP_INTERMEDIATE_0_EP1_HOST=11.11.11.11
export MYAPP_INTERMEDIATE_0_EP1_PORT=6790
export MYAPP_INTERMEDIATE_0_EP2_HOST=22.22.22.22
export MYAPP_INTERMEDIATE_0_EP2_PORT=6791
export MYAPP_INTERMEDIATE_1_EP1_HOST=33.33.33.33
export MYAPP_INTERMEDIATE_1_EP1_PORT=6792
export MYAPP_INTERMEDIATE_1_EP2_HOST=44.44.44.44
export MYAPP_INTERMEDIATE_1_EP2_PORT=6793
```

yields
```scala
      List(
        TwoEndpoints(
          Endpoint("11.11.11.11", 6790),
          Endpoint("22.22.22.22", 6791)
        ),
        TwoEndpoints(
          Endpoint("33.33.33.33", 6792),
          Endpoint("44.44.44.44", 6793)
        )
      )
```

### Option

`Option` configuration uses the `OPT` suffix fragment.

In the example above,
```bash
export MYAPP_ROLE_OPT=somerole
```

yields
```scala
      Some(AppRole("somerole")
```

If no value is present for `MYAPP_ROLE_OPT`, the value is `None`. 

### Either

Similar to `Option`, `Either` configuration uses two suffix fragments:
`C1` for a `Left` value, and `C2` for a `Right` value.

For example, if your app has the following configuration:
```scala
  final case class EitherConfig(
    choice: Either[String, Endpoint]
  )

  object EitherConfig {
    implicit def configured[F[_] : Monad]: Configured[F, EitherConfig] =
      Configured[F, Either[String, Endpoint]].withSuffix("CHOICE")
      .map(EitherConfig.apply)
  }
```

then loading it via:
```scala
      Configured[IO, AppConfig]
        .value("MYAPP")
        .run(Environment.fromEnvVars)
```

with the environment variables:
```bash
export MYAPP_CHOICE_C1=someAppName
```

would yield the following instance of `EitherConfig`:
```scala
      EitherConfig(
        Left("someAppName")
      )
```


but with the environment variables:
```bash
export MYAPP_CHOICE_C2_HOST=12.23.34.45
export MYAPP_CHOICE_C2_PORT=6789
```

would yield the following instance of `EitherConfig`:
```scala
      EitherConfig(
        Right(Endpoint("12.23.34.45,6789"))
      )
```

## Supporting new primitive types

A `Configured` typeclass instance is available for any type that has an instance of the `Conversion` typeclass.
To support another primitive type, such as a Java enum, create an instance of `Conversion`. 

For example, for AWS's `Regions` enum:

```scala
object ConfigSupportAws {
  implicit def conversionRegion: Conversion[Regions] =
    (s: String) => Either.catchNonFatal(Regions.fromName(s))
      .leftMap(_ => s"invalid region $s")

  def configuredRegion[F[_]](defaultRegion: Regions)(implicit F: Applicative[F]): Configured[F, Regions] =
    Configured[F, Option[Regions]]
      .map(_.getOrElse(defaultRegion))
}
```

### Supporting newtypes

Newtypes that wrap an underlying type can easily be created by converting the underlying type and mapping to the newtype.

For example:
```scala
  final case class Latitude(value: Double) extends AnyVal

  object Latitude {
    implicit def conversion: Conversion[Latitude] =
      Conversion[Double].map(Latitude.apply)
  }
```

## Supporting other effects

`List`, `Option`, and `Either` are currently supported. You can add support to your configuration module for other 
effects such as `NonEmptyList` etc. 

## Environment options

Although configuration values are typically read from environment variables, they can be read from any
source that provides an instance of `Environment`:

```scala
trait Environment {
  def get(key: String): Option[String]
}
``` 

`Environment.fromEnvVars` provides normal access to environment variables. 
`Environment.fromMap(map: Map[String, String])` uses a prepopulated map of values, which is useful for unit testing.

## Error reporting

The library is invoked with the `Environment` instance injected via Reader Monad, and returns a `ValidatedNec[ConfiguredError, A]`.
Composition of `Configured` instances is done using applicative combination, eg

```scala
  implicit def configuredf[F[_]](implicit F: Applicative[F]): Configured[F, Endpoint] = (
    Configured[F, String].withSuffix("HOST"),
    Configured[F, Int].withSuffix("PORT")
  ).mapN(Endpoint.apply)
```

This means that if configuration errors are present, all errors are reported, rather than bailing at the first error discovered.

# Release

```bash
VERS=0.4.2
git tag -a v${VERS} -m "v${VERS}"
git push origin v${VERS}
```
