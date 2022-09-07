addSbtPlugin(
  "com.indoorvivants.vcpkg" % "sbt-vcpkg" % sys.env
    .getOrElse("SBT_VCPKG_VERSION", "0.0.6")
)
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.7")

resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin(
  "com.indoorvivants" % "bindgen-sbt-plugin" % "0.0.13+21-8c00bb3f-SNAPSHOT"
)
