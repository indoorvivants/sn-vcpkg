package com.indoorvivants.vcpkg

import scala.util.control.NonFatal

trait VcpkgPluginNativeImpl {
  protected def updateCompilationFlags(
      conf: VcpkgNativeConfig,
      oldFlags: Seq[String],
      newFlags: Seq[String]
  ) =
    if (!conf.autoConfigure) oldFlags
    else if (conf.prependCompileOptions) newFlags ++ oldFlags
    else oldFlags ++ newFlags

  protected def updateLinkingFlags(
      conf: VcpkgNativeConfig,
      oldFlags: Seq[String],
      newFlags: Seq[String]
  ) =
    if (!conf.autoConfigure) oldFlags
    else if (conf.prependLinkingOptions) newFlags ++ oldFlags
    else oldFlags ++ newFlags

  protected def compilationFlags(
      configurator: VcpkgConfigurator,
      deps: List[String],
      logger: ExternalLogger,
      conf: VcpkgNativeConfig
  ) = {

    val result = Seq.newBuilder[String]

    deps.foreach { dep =>
      val files = configurator.files(dep)
      val compileArgsApprox =
        List("-I" + files.includeDir.toString)

      val nameOverride = conf.renamedLibraries.get(dep)

      val nameDescr = nameOverride.getOrElse(dep) + nameOverride
        .map(_ => s" (renamed from `$dep`)")
        .getOrElse("")

      try {
        result ++= configurator.pkgConfig.compilationFlags(
          nameOverride.getOrElse(dep)
        )
      } catch {
        case NonFatal(exc) =>
          if (conf.approximate) {
            logger.warn(
              s"Compilation flags for `${nameDescr}` dependency were approximate, and might be incorrect. " +
                "If you want to disable approximation, set `vcpkgNativeApprxomate := false`"
            )
            result ++= compileArgsApprox
          } else {
            logger.warn(
              s"Failed to retrieve compilation flags for `$dep`, most likely due to missing pkg-config file"
            )
          }
      }
    }
    result.result()
  }

  protected def linkingFlags(
      configurator: VcpkgConfigurator,
      deps: List[String],
      logger: ExternalLogger,
      conf: VcpkgNativeConfig
  ) = {

    val result = Seq.newBuilder[String]

    deps.foreach { dep =>
      val files = configurator.files(dep)

      val linkingArgsApprox =
        List("-L" + files.libDir) ++ files.staticLibraries.map(_.toString)

      val nameOverride = conf.renamedLibraries.get(dep)

      val nameDescr = nameOverride.getOrElse(dep) + nameOverride
        .map(_ => s" (renamed from `$dep`)")
        .getOrElse("")


      try {
        result ++= configurator.pkgConfig.linkingFlags(nameOverride.getOrElse(dep))
      } catch {
        case NonFatal(exc) =>
          if (conf.approximate) {
            logger.warn(
              s"Linking flags for `$nameDescr` dependency were approximated, and might be incorrect. " +
                "If you want to disable approximation, set `vcpkgNativeApprxomate := false`"
            )
            result ++= linkingArgsApprox
          } else {
            logger.warn(
              s"Failed to retrieve linking flags for `$nameDescr`, most likely due to missing pkg-config file"
            )
          }
      }
    }
    result.result()
  }
}
