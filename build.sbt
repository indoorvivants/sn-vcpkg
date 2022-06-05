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
lazy val supportedScalaVersions = List(scala213, scala212)
ThisBuild / crossScalaVersions := supportedScalaVersions

lazy val root = (project in file("."))
  .aggregate(core, `sbt-plugin`)
  .settings(
    // crossScalaVersions must be set to Nil on the aggregating project
    crossScalaVersions := Nil,
    publish / skip := true
  )

lazy val core = project
  .in(file("core"))
  .settings(
    name := "vcpkg-core",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "6.1.0.202203080745-r"
  )

lazy val `sbt-plugin` = project
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
    crossScalaVersions := Seq(scala212)
  )

console / initialCommands := """import com.indoorvivants.sbt._"""

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(RefPredicate.StartsWith(Ref.Tag("v")))

ThisBuild / githubWorkflowJavaVersions := Seq(
  sbtghactions.JavaSpec.temurin("11")
)

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("test")),
  WorkflowStep.Sbt(
    List("scripted"),
    cond = Some("startsWith(matrix.scala, '2.12')")
  )
)

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

Global / onChangedBuildSource := ReloadOnSourceChanges
