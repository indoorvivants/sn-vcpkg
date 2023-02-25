package com.indoorvivants.vcpkg

class VcpkgNativeConfig private (
    val autoConfigure: Boolean = true,
    val approximate: Boolean = true,
    val staticLinking: Boolean = false,
    val prependCompileOptions: Boolean = true,
    val prependLinkingOptions: Boolean = true,
    val renamedLibraries: Map[String, String] = Map.empty
) { self =>
  private def copy(
      autoConfigure: Boolean = self.autoConfigure,
      approximate: Boolean = self.approximate,
      staticLinking: Boolean = self.staticLinking,
      prependCompileOptions: Boolean = self.prependCompileOptions,
      prependLinkingOptions: Boolean = self.prependLinkingOptions,
      renamedLibraries: Map[String, String] = self.renamedLibraries
  ) =
    new VcpkgNativeConfig(
      autoConfigure = autoConfigure,
      approximate = approximate,
      staticLinking = staticLinking,
      prependCompileOptions = prependCompileOptions,
      prependLinkingOptions = prependLinkingOptions,
      renamedLibraries = renamedLibraries
    )

  def withAutoConfigure(value: Boolean) = copy(autoConfigure = value)
  def withApproximate(value: Boolean) = copy(approximate = value)
  def withStaticLinking(value: Boolean) = copy(staticLinking = value)
  def withPrependCompileOptions(value: Boolean) =
    copy(prependCompileOptions = value)
  def withPrependLinkingOptions(value: Boolean) =
    copy(prependLinkingOptions = value)

  def withRenamedLibraries(renamedLibraries: Map[String, String]) =
    copy(renamedLibraries = renamedLibraries)
  def addRenamedLibrary(vcpkgName: String, pkgConfigName: String) =
    copy(renamedLibraries =
      self.renamedLibraries.updated(vcpkgName, pkgConfigName)
    )

  private def toMap: Map[String, String] =
    Map(
      "autoConfigure" -> autoConfigure.toString,
      "approximate" -> approximate.toString,
      "staticLinking" -> staticLinking.toString,
      "prependCompileOptions" -> prependCompileOptions.toString,
      "prependLinkingOptions" -> prependLinkingOptions.toString,
      "renamedLibraries" -> renamedLibraries.toString
    )

  override def toString() =
    "Vcpkg NativeConfig: \n" + toMap.toSeq
      .sortBy(_._1)
      .map { case (k, v) =>
        s"  | $k = $v"
      }
      .mkString("\n")

}

object VcpkgNativeConfig {
  def apply(): VcpkgNativeConfig = new VcpkgNativeConfig()
}
