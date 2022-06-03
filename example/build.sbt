enablePlugins(VcpkgPlugin, ScalaNativePlugin)

vcpkgDependencies := Set("libuv", "czmq", "libpq")

nativeConfig := {
  val conf = nativeConfig.value

  conf
    .withCompileOptions(
      conf.compileOptions ++ vcpkgCompilationArguments.value
    )
    .withLinkingOptions(
      conf.linkingOptions ++ vcpkgLinkingArguments.value
    )
}

scalaVersion := "3.1.2"
