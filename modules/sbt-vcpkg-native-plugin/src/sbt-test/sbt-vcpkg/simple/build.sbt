version := "0.1"
scalaVersion := "3.2.2"

enablePlugins(VcpkgNativePlugin, ScalaNativePlugin)

vcpkgDependencies := VcpkgDependencies(
  "cjson",
  "cmark"
)

vcpkgNativeConfig ~= {
  _.withRenamedLibraries(
    Map("cjson" -> "libcjson", "cmark" -> "libcmark")
  )
}
