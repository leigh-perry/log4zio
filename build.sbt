import Dependencies._

val Scala_213 = "2.13.8"
val Scala_212 = "2.12.16"
val Scala_211 = "2.11.12"

////

val projectName = "log4zio"

inThisBuild(
  List(
    organization := "com.github.leigh-perry",
    homepage := Some(url(s"https://github.com/leigh-perry/${projectName.toLowerCase}")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers :=
      List(
        Developer(
          "leigh-perry",
          "Leigh Perry",
          "lperry.breakpoint@gmail.com",
          url("https://leigh-perry.github.io")
        )
      )
  )
)

lazy val compilerPlugins =
  List(
    compilerPlugin("org.typelevel" %% "kind-projector" % Version.kindProjectorVersion)
  )

lazy val commonSettings =
  Seq(
    scalaVersion := Scala_213,
    scalacOptions ++= commonScalacOptions(scalaVersion.value),
    fork in Test := true,
    testFrameworks += TestFramework("org.scalacheck.ScalaCheckFramework"),
    name := projectName,
    updateOptions := updateOptions.value.withGigahorse(false),
    libraryDependencies ++=
      Seq(
        zio,
        zioTest % "test",
        zioTestSbt % "test"
      ) ++ compilerPlugins
  )

lazy val crossBuiltCommonSettings =
  commonSettings ++
    Seq(crossScalaVersions := Seq(Scala_211, Scala_212, Scala_213))

lazy val core =
  module("core")
    .settings(
      libraryDependencies ++=
        Seq(
          )
    )

lazy val slf4j =
  module("slf4j")
    .settings(
      libraryDependencies ++=
        Seq(
          slf4jApi
        )
    )
    .dependsOn(core % "compile->compile;test->test")

lazy val exampleApps =
  subproject(dir = "examples", spName = "apps")
    .settings(
      libraryDependencies ++=
        Seq(
          logback
        )
    )
    .dependsOn(
      core % "compile->compile;test->test",
      slf4j % "compile->compile;test->test"
    )

lazy val allModules = List(core, slf4j, exampleApps)

lazy val root =
  project
    .in(file("."))
    .settings(commonSettings)
    .settings(skip in publish := true, crossScalaVersions := List())
    .aggregate((allModules).map(x => x: ProjectReference): _*)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("fmtcheck", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

////

def module(moduleName: String): Project =
  subproject("modules", moduleName)
    .settings(name += s"-$moduleName") // for artifact naming

def subproject(dir: String, spName: String): Project =
  Project(spName, file(s"$dir/$spName"))
    .settings(crossBuiltCommonSettings)

def versionDependentExtraScalacOptions(scalaVersion: String) =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 =>
      Seq("-Yno-adapted-args", "-Xfuture", "-Ypartial-unification")
    case _ => Nil
  }

def commonScalacOptions(scalaVersion: String) =
  Seq(
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    //"-Xfatal-warnings",
    "-deprecation"
    //"-Xlint:-unused,_"
  ) ++
    versionDependentExtraScalacOptions(scalaVersion)
