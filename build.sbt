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

val V = new {
  val scala213 = "2.13.9"

  val scala212 = "2.12.16"

  val scala3 = "3.2.0"

  val dirs = "26"

  val detective = "0.0.2"

  val eclipseGit = "6.3.0.202209071007-r"

  val mill = "0.10.7"

  val utest = "0.8.1"

  val supportedScalaVersions = List(scala213, scala212, scala3)
}

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
  .jvmPlatform(scalaVersions = V.supportedScalaVersions)
  .in(file("core"))
  .settings(publishing)
  .settings(
    name := "vcpkg-core",
    libraryDependencies += "dev.dirs" % "directories" % V.dirs,
    libraryDependencies += "com.indoorvivants.detective" %% "platform" % V.detective,
    crossScalaVersions := V.supportedScalaVersions,
    libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % V.eclipseGit,
    scalacOptions += "-Xsource:3"
  )

lazy val `sbt-plugin` = projectMatrix
  .jvmPlatform(scalaVersions = Seq(V.scala212))
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
  .jvmPlatform(scalaVersions = Seq(V.scala213))
  .in(file("mill-plugin"))
  .dependsOn(core)
  .settings(publishing)
  .settings(
    name := """mill-vcpkg""",
    libraryDependencies += "com.lihaoyi" %% "mill-scalalib" % V.mill,
    libraryDependencies += "com.lihaoyi" %% "utest" % V.utest % Test,
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val versionDump =
  taskKey[Unit]("Dumps the version in a file named version")

versionDump := {
  val file = (ThisBuild / baseDirectory).value / "version"
  IO.write(file, (Compile / version).value)
}
