val BindgenVersion =
  sys.env.getOrElse("SN_BINDGEN_VERSION", "0.0.16")

val VcpkgVersion =
  sys.env.getOrElse("SBT_VCPKG_VERSION", "0.0.9")

addSbtPlugin("com.indoorvivants" % "bindgen-sbt-plugin" % BindgenVersion)
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.7")
addSbtPlugin("com.indoorvivants.vcpkg" % "sbt-vcpkg-native" % VcpkgVersion)

resolvers += Resolver.sonatypeRepo("snapshots")
