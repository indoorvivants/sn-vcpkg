package com.indoorvivants.vcpkg.millplugin.native

import com.indoorvivants.vcpkg.VcpkgPluginNativeImpl
import com.indoorvivants.vcpkg.millplugin.VcpkgModule
import mill._, scalalib._, scalanativelib._, mill.scalanativelib.api._
import com.indoorvivants.vcpkg.{
  VcpkgNativeConfig,
  VcpkgManifestFile,
  VcpkgManifestDependency
}

private[native] object manifestReader {
  implicit val rw: upickle.default.Reader[VcpkgManifestDependency] =
    upickle.default.macroR[VcpkgManifestDependency]
  implicit val rwMF: upickle.default.Reader[VcpkgManifestFile] =
    upickle.default.macroR[VcpkgManifestFile]

  def apply(contents: String): VcpkgManifestFile =
    upickle.default.read[VcpkgManifestFile](contents)
}

trait VcpkgNativeModule
    extends ScalaNativeModule
    with VcpkgModule
    with VcpkgPluginNativeImpl {

  def vcpkgNativeLinking: T[Seq[String]] = Task {
    linkingFlags(
      configurator = vcpkgConfigurator(),
      deps =
        vcpkgDependencies().dependencies(manifestReader.apply).map(_.short),
      logger = millLogger(Task.log),
      conf = vcpkgNativeConfig()
    )
  }

  def vcpkgNativeCompilation: T[Seq[String]] = Task {
    compilationFlags(
      configurator = vcpkgConfigurator(),
      deps =
        vcpkgDependencies().dependencies(manifestReader.apply).map(_.short),
      logger = millLogger(Task.log),
      conf = vcpkgNativeConfig()
    )
  }

  def vcpkgNativeConfig: Task[VcpkgNativeConfig] = Task.Anon {
    VcpkgNativeConfig()
  }

  override def nativeCompileOptions: T[Seq[String]] = Task {
    updateCompilationFlags(
      vcpkgNativeConfig(),
      super.nativeCompileOptions(),
      vcpkgNativeCompilation()
    )
  }

  override def nativeLinkingOptions: T[Seq[String]] = Task {
    updateLinkingFlags(
      vcpkgNativeConfig(),
      super.nativeLinkingOptions(),
      vcpkgNativeLinking()
    )

  }
}
