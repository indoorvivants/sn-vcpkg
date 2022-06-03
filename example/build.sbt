enablePlugins(VcpkgPlugin, ScalaNativePlugin, BindgenPlugin)

vcpkgDependencies := Set("libuv", "czmq", "cjson")

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

import bindgen.interface.Binding

bindgenBindings := Seq(
  Binding(
    vcpkgManager.value.includes("cjson") / "cjson" / "cJSON.h",
    "cjson",
    cImports = List("cJSON.h")
  ),
  Binding(
    vcpkgManager.value.includes("libuv") / "uv.h",
    "libuv",
    cImports = List("uv.h"),
    clangFlags = List("-I" + vcpkgManager.value.includes("libuv").toString)
  ),
  Binding(
    vcpkgManager.value.includes("czmq") / "czmq.h",
    "czmq",
    cImports = List("czmq.h"),
    clangFlags = List(
      "-I" + vcpkgManager.value.includes("czmq").toString,
      "-I" + vcpkgManager.value.includes("zeromq").toString
    )
  )
)
