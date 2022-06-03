// DO NOT EDIT! This file is auto-generated.

// This plugin enables semantic information to be produced by sbt.
// It also adds support for debugging using the Debug Adapter Protocol
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
addSbtPlugin("org.scalameta" % "sbt-metals" % "0.11.5+157-d0cf15ce-SNAPSHOT")

// This plugin adds the BSP debug capability to sbt server.

addSbtPlugin("ch.epfl.scala" % "sbt-debug-adapter" % "2.2.0-M1")

