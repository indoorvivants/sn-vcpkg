package com.indoorvivants.vcpkg

import scala.util.control.NonFatal

trait VcpkgPluginNativeImpl {
  protected def compilationFlags(
      configurator: VcpkgConfigurator,
      deps: Seq[String],
      logger: ExternalLogger,
      approximate: Boolean
  ) = {

    val result = Seq.newBuilder[String]

    deps.foreach { dep =>
      val files = configurator.files(dep)
      val compileArgsApprox =
        List("-I" + files.includeDir.toString)

      try {
        result ++= configurator.pkgConfig.compilationFlags(dep)
      } catch {
        case NonFatal(exc) =>
          if (approximate) {
            logger.warn(
              s"Compilation flags for `$dep` dependency were approximate, and might be incorrect. " +
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
      deps: Seq[String],
      logger: ExternalLogger,
      approximate: Boolean
  ) = {

    val result = Seq.newBuilder[String]

    deps.foreach { dep =>
      val files = configurator.files(dep)

      val linkingArgsApprox =
        List("-L" + files.libDir) ++ files.staticLibraries.map(_.toString)

      try {
        result ++= configurator.pkgConfig.linkingFlags(dep)
      } catch {
        case NonFatal(exc) =>
          if (approximate) {
            logger.warn(
              s"Linking flags for `$dep` dependency were approximated, and might be incorrect. " +
                "If you want to disable approximation, set `vcpkgNativeApprxomate := false`"
            )
            result ++= linkingArgsApprox
          } else {
            logger.warn(
              s"Failed to retrieve linking flags for `$dep`, most likely due to missing pkg-config file"
            )
          }
      }
    }
    result.result()
  }
}
