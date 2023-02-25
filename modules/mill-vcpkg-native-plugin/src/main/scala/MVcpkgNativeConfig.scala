package com.indoorvivants.vcpkg.millplugin

import mill._
import upickle.default._
import com.indoorvivants.vcpkg.Vcpkg
import com.indoorvivants.vcpkg._

private[vcpkg] case class MVcpkgNativeConfig(
    autoConfigure: Boolean,
    approximate: Boolean,
    staticLinking: Boolean,
    prependCompileOptions: Boolean,
    prependLinkingOptions: Boolean,
    renamedLibraries: Map[String, String]
) {
  def toVcpkg: VcpkgNativeConfig =
    VcpkgNativeConfig()
      .withAutoConfigure(autoConfigure)
      .withApproximate(approximate)
      .withStaticLinking(staticLinking)
      .withPrependCompileOptions(prependCompileOptions)
      .withPrependLinkingOptions(prependLinkingOptions)
      .withRenamedLibraries(renamedLibraries)
}

private[vcpkg] object MVcpkgNativeConfig {

  def fromVcpkg(v: VcpkgNativeConfig) =
    MVcpkgNativeConfig(
      autoConfigure = v.autoConfigure,
      approximate = v.approximate,
      staticLinking = v.staticLinking,
      prependCompileOptions = v.prependCompileOptions,
      prependLinkingOptions = v.prependLinkingOptions,
      renamedLibraries = v.renamedLibraries
    )

  implicit val readWriter: ReadWriter[MVcpkgNativeConfig] =
    macroRW[MVcpkgNativeConfig]
}
