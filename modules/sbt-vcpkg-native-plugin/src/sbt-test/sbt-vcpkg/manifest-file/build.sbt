version := "0.1"
scalaVersion := "3.2.0"

enablePlugins(VcpkgNativePlugin)

vcpkgDependencies := VcpkgDependencies(file("vcpkg.json"))

vcpkgNativeConfig ~= {
  _.withRenamedLibraries(
    Map("cjson" -> "libcjson", "cmark" -> "libcmark")
  )
}
