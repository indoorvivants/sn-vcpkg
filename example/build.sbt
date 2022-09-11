enablePlugins(VcpkgPlugin, ScalaNativePlugin, BindgenPlugin)

vcpkgDependencies := Set("libuv", "czmq", "cjson", "zeromq")

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "3.2.0"

import bindgen.interface.Binding

bindgenBindings := {
  val configurator = vcpkgConfigurator.value
  Seq(
    Binding(
      configurator.includes("cjson") / "cjson" / "cJSON.h",
      "cjson",
      cImports = List("cJSON.h")
    ),
    Binding(
      configurator.includes("libuv") / "uv.h",
      "libuv",
      cImports = List("uv.h"),
      clangFlags = List("-I" + configurator.includes("libuv").toString)
    ),
    Binding(
      configurator.includes("czmq") / "czmq.h",
      "czmq",
      cImports = List("czmq.h"),
      clangFlags = List(
        "-I" + configurator.includes("czmq").toString,
        "-I" + configurator.includes("zeromq").toString
      )
    )
  )
}

nativeConfig := {
  import com.indoorvivants.detective.Platform
  val configurator = vcpkgConfigurator.value
  val pkgConfig = configurator.pkgConfig
  val conf = nativeConfig.value
  val deps = vcpkgDependencies.value.toSeq

  val files = deps.map(d => configurator.files(d))

  val compileArgsApprox = files.flatMap { f =>
    List("-I" + f.includeDir.toString)
  }
  val linkingArgsApprox = files.flatMap { f =>
    List("-L" + f.libDir) ++ f.staticLibraries.map(_.toString)
  }

  import scala.util.control.NonFatal

  def updateLinkingFlags(current: Seq[String], deps: String*) =
    try {
      pkgConfig.updateLinkingFlags(
        current,
        deps *
      )
    } catch {
      case NonFatal(exc) =>
        linkingArgsApprox
    }

  def updateCompilationFlags(current: Seq[String], deps: String*) =
    try {
      pkgConfig.updateCompilationFlags(
        current,
        deps *
      )
    } catch {
      case NonFatal(exc) =>
        compileArgsApprox
    }

  val arch64 =
    if (
      Platform.arch == Platform.Arch.Arm && Platform.bits == Platform.Bits.x64
    )
      List("-arch", "arm64")
    else Nil

  conf
    .withLinkingOptions(
      updateLinkingFlags(
        conf.linkingOptions ++ arch64,
        deps *
      )
    )
    .withCompileOptions(
      updateCompilationFlags(
        conf.compileOptions ++ arch64,
        deps *
      )
    )
}
