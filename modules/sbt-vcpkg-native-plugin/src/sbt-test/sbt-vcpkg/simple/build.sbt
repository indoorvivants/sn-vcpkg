version := "0.1"
scalaVersion := "3.3.8"

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
