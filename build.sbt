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
  val scala213 = "2.13.10"

  val scala212 = "2.12.16"

  val scala3 = "3.2.2"

  val dirs = "26"

  val detective = "0.0.2"

  val eclipseGit = "6.4.0.202211300538-r"

  val mill = "0.10.11"

  val utest = "0.8.1"

  val weaver = "0.8.1"

  val b2s = "0.3.17"

  val decline = "2.4.1"

  val scribe = "3.11.1"

  val supportedScalaVersions = List(scala213, scala212, scala3)
}

lazy val publishing = Seq(
  organization := "com.indoorvivants.vcpkg",
  sonatypeProfileName := "com.indoorvivants"
)

lazy val root = project
  .in(file("."))
  .aggregate(core.projectRefs *)
  .aggregate(`sbt-vcpkg-plugin`.projectRefs *)
  .aggregate(`sbt-vcpkg-native-plugin`.projectRefs *)
  .aggregate(`mill-vcpkg-plugin`.projectRefs *)
  .aggregate(cli.projectRefs *)
  .settings(
    publish / skip := true
  )

lazy val core = projectMatrix
  .jvmPlatform(scalaVersions = V.supportedScalaVersions)
  .in(file("modules/core"))
  .settings(publishing)
  .settings(
    name := "vcpkg-core",
    libraryDependencies ++= Seq(
      "dev.dirs" % "directories" % V.dirs,
      "com.indoorvivants.detective" %% "platform" % V.detective,
      "org.eclipse.jgit" % "org.eclipse.jgit" % V.eclipseGit,
      "com.disneystreaming" %% "weaver-cats" % V.weaver % Test
    ),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    scalacOptions ++= {
      if (!scalaVersion.value.startsWith("3.")) Seq("-Xsource:3") else Seq.empty
    }
  )

lazy val cli = projectMatrix
  .jvmPlatform(scalaVersions = Seq(V.scala3))
  .defaultAxes(VirtualAxis.scalaABIVersion(V.scala3), VirtualAxis.jvm)
  .dependsOn(core)
  .in(file("modules/cli"))
  .settings(publishing)
  .settings(
    name := "scala-vcpkg",
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    libraryDependencies += "com.monovore" %% "decline" % V.decline,
    libraryDependencies += "com.outr" %% "scribe" % V.scribe
  )

lazy val `sbt-vcpkg-plugin` = projectMatrix
  .jvmPlatform(scalaVersions = Seq(V.scala212))
  .in(file("modules/sbt-vcpkg-plugin"))
  .dependsOn(core)
  .enablePlugins(ScriptedPlugin, SbtPlugin)
  .settings(publishing)
  .settings(
    name := "sbt-vcpkg",
    sbtPlugin := true,
    // set up 'scripted; sbt plugin for testing sbt plugins
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    ),
    scriptedBufferLog := false
  )

lazy val `sbt-vcpkg-native-plugin` = projectMatrix
  .jvmPlatform(scalaVersions = Seq(V.scala212))
  .in(file("modules/sbt-vcpkg-native-plugin"))
  .dependsOn(core, `sbt-vcpkg-plugin`)
  .enablePlugins(ScriptedPlugin, SbtPlugin)
  .settings(publishing)
  .settings(
    name := """sbt-vcpkg-native""",
    sbtPlugin := true,
    // set up 'scripted; sbt plugin for testing sbt plugins
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    ),
    addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.10"),
    scriptedBufferLog := false
  )

lazy val `mill-vcpkg-plugin` = projectMatrix
  .jvmPlatform(scalaVersions = Seq(V.scala213))
  .in(file("modules/mill-vcpkg-plugin"))
  .dependsOn(core)
  .settings(publishing)
  .settings(
    name := "mill-vcpkg",
    libraryDependencies += "com.lihaoyi" %% "mill-scalalib" % V.mill,
    libraryDependencies += "com.lihaoyi" %% "utest" % V.utest % Test,
    testFrameworks += new TestFramework("utest.runner.Framework"),
    Test / fork := true,
    Test / envVars := Map(
      "MILL_VCPKG_ROOT" -> ((ThisBuild / baseDirectory).value / "mill-vcpkg-plugin" / "src" / "test").toString
    )
  )

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val versionDump =
  taskKey[Unit]("Dumps the version in a file named version")

versionDump := {
  val file = (ThisBuild / baseDirectory).value / "version"
  IO.write(file, (Compile / version).value)
}
