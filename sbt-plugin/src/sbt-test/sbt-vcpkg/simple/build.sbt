version := "0.1"
scalaVersion := "2.12.15"

enablePlugins(VcpkgPlugin)

vcpkgDependencies := Set("libuv", "cjson")

val testPkgConfig = taskKey[Unit]("")

testPkgConfig := {
  val pkgConfig = vcpkgConfigurator.value

  val compilation = pkgConfig.compilationFlags("libuv", "libcjson")
  val linking = pkgConfig.linkingFlags("libuv", "libcjson")

  assert(compilation.exists(_.contains("cjson")))
  assert(compilation.exists(_.contains("libuv")))

  assert(linking.exists(_.contains("-luv")))
  assert(linking.exists(_.contains("-lcjson")))
}
