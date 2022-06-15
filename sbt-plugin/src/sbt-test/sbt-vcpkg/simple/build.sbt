version := "0.1"
scalaVersion := "2.12.15"

enablePlugins(VcpkgPlugin)

vcpkgDependencies := Set("libuv", "cjson")
