addSbtPlugin(
  "com.indoorvivants" % "sbt-vcpkg" % sys.env
    .getOrElse("SBT_VCPKG_VERSION", "0.0.3")
)
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.4")

resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin(
  "com.indoorvivants" % "bindgen-sbt-plugin" % "0.0.9+2-668a1f06-SNAPSHOT"
)
