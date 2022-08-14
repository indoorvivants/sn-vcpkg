inThisBuild(
  List(
    homepage := Some(url("https://github.com/indoorvivants/sbt-vcpkg")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "indoorvivants",
        "Anton Sviridov",
        "contact@indoorvivants.com",
        url("https://blog.indoorvivants.com")
      )
    ),
    version := (if (!sys.env.contains("CI")) "dev" else version.value),
    crossScalaVersions := Nil
  )
)

organization := "com.indoorvivants.vcpkg"
sonatypeProfileName := "com.indoorvivants"

lazy val scala213 = "2.13.8"
lazy val scala212 = "2.12.16"
lazy val scala3 = "3.1.3"
lazy val supportedScalaVersions = List(scala213, scala212, scala3)

lazy val publishing = Seq(
  organization := "com.indoorvivants.vcpkg",
  sonatypeProfileName := "com.indoorvivants"
)

lazy val root = project
  .in(file("."))
  .aggregate(
    (core.projectRefs ++ `sbt-plugin`.projectRefs ++ `mill-plugin`.projectRefs) *
  )
  .settings(
    publish / skip := true
  )

lazy val core = projectMatrix
  .jvmPlatform(scalaVersions = supportedScalaVersions)
  .in(file("core"))
  .settings(publishing)
  .settings(
    name := "vcpkg-core",
    libraryDependencies += "dev.dirs" % "directories" % "26",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "6.1.0.202203080745-r",
    scalacOptions += "-Xsource:3"
  )

lazy val `sbt-plugin` = projectMatrix
  .jvmPlatform(scalaVersions = Seq(scala212))
  .in(file("sbt-plugin"))
  .dependsOn(core)
  .enablePlugins(ScriptedPlugin, SbtPlugin)
  .settings(publishing)
  .settings(
    name := """sbt-vcpkg""",
    sbtPlugin := true,
    // set up 'scripted; sbt plugin for testing sbt plugins
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    ),
    scriptedBufferLog := false
  )

lazy val `mill-plugin` = projectMatrix
  .jvmPlatform(scalaVersions = Seq(scala213))
  .in(file("mill-plugin"))
  .dependsOn(core)
  .settings(publishing)
  .settings(
    name := """mill-vcpkg""",
    libraryDependencies += "com.lihaoyi" %% "mill-scalalib" % "0.10.4",
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.11" % Test,
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
