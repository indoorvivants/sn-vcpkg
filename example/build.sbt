enablePlugins(VcpkgNativePlugin, ScalaNativePlugin, BindgenPlugin)

vcpkgDependencies := VcpkgDependencies(
  "libuv",
  "czmq",
  "cjson",
  "zeromq"
)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "3.6.3"

import bindgen.interface.Binding

vcpkgNativeConfig ~= {
  _.withRenamedLibraries(
    Map("zeromq" -> "libzmq", "czmq" -> "libczmq", "cjson" -> "libcjson")
  )
}

bindgenBindings := {
  val configurator = vcpkgConfigurator.value
  Seq(
    Binding(configurator.includes("cjson") / "cjson" / "cJSON.h", "cjson")
      .withCImports(List("cJSON.h")),
    Binding(configurator.includes("libuv") / "uv.h", "libuv")
      .withCImports(List("uv.h"))
      .withClangFlags(
        List(
          "-I" + configurator.includes("libuv").toString
        )
      ),
    Binding(configurator.includes("czmq") / "czmq.h", "czmq")
      .withCImports(List("czmq.h"))
      .withClangFlags(
        List(
          "-I" + configurator.includes("czmq").toString,
          "-I" + configurator.includes("zeromq").toString
        )
      )
  )
}
