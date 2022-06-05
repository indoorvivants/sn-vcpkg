inThisBuild(
  List(
    organization := "com.indoorvivants", // TODO : org should probably be com.indoorvivants.vcpkg
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
    )
  )
)

lazy val scala213 = "2.13.8"
lazy val scala212 = "2.12.15"
lazy val scala3 = "3.1.2"
lazy val supportedScalaVersions = List(scala213, scala212, scala3)

lazy val root = project
  .in(file("."))
  .aggregate((core.projectRefs ++ `sbt-plugin`.projectRefs) *)
  .settings(
    publish / skip := true
  )

lazy val core = projectMatrix
  .jvmPlatform(scalaVersions = supportedScalaVersions)
  .in(file("core"))
  .settings(
    name := "vcpkg-core",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "6.1.0.202203080745-r",
    scalacOptions += "-Xsource:3"
  )

lazy val `sbt-plugin` = projectMatrix
  .jvmPlatform(scalaVersions = Seq(scala212))
  .in(file("sbt-plugin"))
  .dependsOn(core)
  .enablePlugins(ScriptedPlugin)
  .settings(
    name := """sbt-vcpkg""",
    sbtPlugin := true,
    // set up 'scripted; sbt plugin for testing sbt plugins
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    ),
    scriptedBufferLog := false,
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
