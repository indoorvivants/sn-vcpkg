enablePlugins(VcpkgNativePlugin, ScalaNativePlugin, BindgenPlugin)

vcpkgDependencies := Set("libuv", "czmq", "cjson", "zeromq")

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "3.2.2"

import bindgen.interface.Binding

vcpkgNativeConfig ~= {
  _.withRenamedLibraries(
    Map("zeromq" -> "libzmq", "czmq" -> "libczmq", "cjson" -> "libcjson")
  )
}

bindgenBindings := {
  val configurator = vcpkgConfigurator.value
  Seq(
    Binding
      .builder(configurator.includes("cjson") / "cjson" / "cJSON.h", "cjson")
      .withCImports(List("cJSON.h"))
      .build,
    Binding
      .builder(configurator.includes("libuv") / "uv.h", "libuv")
      .withCImports(List("uv.h"))
      .withClangFlags(
        List(
          "-I" + configurator.includes("libuv").toString
        )
      )
      .build,
    Binding
      .builder(configurator.includes("czmq") / "czmq.h", "czmq")
      .withCImports(List("czmq.h"))
      .withClangFlags(
        List(
          "-I" + configurator.includes("czmq").toString,
          "-I" + configurator.includes("zeromq").toString
        )
      )
      .build
  )
}
