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
  val scala213 = "2.13.12"

  val scala212 = "2.12.18"

  val scala3 = "3.3.3"

  val dirs = "26"

  val detective = "0.0.2"

  val eclipseGit = "6.4.0.202211300538-r"

  val mill = "0.10.15"

  val utest = "0.8.2"

  val weaver = "0.8.4"

  val b2s = "0.3.17"

  val decline = "2.4.1"

  val scribe = "3.12.2"

  val scalaNative = "0.4.17"

  val circe = "0.14.6"

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
  .aggregate(`mill-vcpkg-native-plugin`.projectRefs *)
  .aggregate(cli.projectRefs *)
  .settings(
    publish / skip := true
  )

lazy val docs =
  project
    .in(file("_docs"))
    .enablePlugins(MdocPlugin)
    .dependsOn(core.jvm(V.scala3), cli.jvm(V.scala3))
    .settings(scalaVersion := V.scala3)
    .settings(
      mdocVariables := Map("VERSION" -> "0.0.20", "SCALA3_VERSION" -> V.scala3)
    )

lazy val docsDrifted = taskKey[Boolean]("")
docsDrifted := {
  val readmeIn = (baseDirectory.value / "README.in.md").toString
  val generated =
    (docs / Compile / mdoc).toTask(s"").value

  val out = (docs / Compile / mdocOut).value / "README.in.md"

  val actualReadme = (ThisBuild / baseDirectory).value / "README.md"

  val renderedContents = IO.read(out)
  val actualContents = IO.read(actualReadme)

  renderedContents != actualContents
}

lazy val checkDocs = taskKey[Unit]("")
checkDocs := {

  val hasDrifted = docsDrifted.value

  if (hasDrifted) {
    throw new MessageOnlyException(
      "Docs have drifted! please run `updateDocs` in SBT to rectify"
    )
  }
}

lazy val updateDocs = taskKey[Unit]("")
updateDocs := {

  val hasDrifted = docsDrifted.value

  if (hasDrifted) {
    sLog.value.warn("README.md has drifted, overwriting it")

    val out = (docs / Compile / mdocOut).value / "README.in.md"

    val actualReadme = (ThisBuild / baseDirectory).value / "README.md"

    IO.copyFile(out, actualReadme)
  } else {

    sLog.value.info("README.md is up to date")
  }
}

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
    name := "sn-vcpkg",
    run / fork := true,
    run / baseDirectory := (ThisBuild / baseDirectory).value,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    libraryDependencies += "com.monovore" %% "decline" % V.decline,
    libraryDependencies += "io.circe" %% "circe-parser" % V.circe,
    libraryDependencies += "com.outr" %% "scribe" % V.scribe
  )

lazy val `sbt-vcpkg-plugin` = projectMatrix
  .jvmPlatform(scalaVersions = Seq(V.scala212))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(V.scala212))
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
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(V.scala212))
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
    addSbtPlugin("org.scala-native" % "sbt-scala-native" % V.scalaNative),
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
    Test / envVars += (
      "MILL_VCPKG_ROOT" -> ((ThisBuild / baseDirectory).value / "modules" / "mill-vcpkg-plugin" / "src" / "test").toString
    )
  )

lazy val `mill-vcpkg-native-plugin` = projectMatrix
  .jvmPlatform(scalaVersions = Seq(V.scala213))
  .in(file("modules/mill-vcpkg-native-plugin"))
  .dependsOn(core, `mill-vcpkg-plugin` % "test->test;compile->compile")
  .settings(publishing)
  .settings(
    name := "mill-vcpkg",
    libraryDependencies += "com.lihaoyi" %% "mill-scalanativelib" % V.mill,
    libraryDependencies += "com.lihaoyi" %% "utest" % V.utest % Test,
    testFrameworks += new TestFramework("utest.runner.Framework"),
    Test / fork := true,
    Test / envVars += (
      "MILL_VCPKG_ROOT" -> ((ThisBuild / baseDirectory).value / "modules" / "mill-vcpkg-native-plugin" / "src" / "test").toString
    )
  )

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val versionDump =
  taskKey[Unit]("Dumps the version in a file named `version`")

versionDump := {
  val file = (ThisBuild / baseDirectory).value / "version"
  IO.write(file, (Compile / version).value)
}

addCommandAlias(
  "pluginTests",
  "sbt-vcpkg-plugin/scripted;sbt-vcpkg-native-plugin/scripted"
)
