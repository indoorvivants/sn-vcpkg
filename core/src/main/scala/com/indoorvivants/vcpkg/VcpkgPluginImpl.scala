package com.indoorvivants.vcpkg

import Platform.OS._
import java.io.File

/** A bunch of build-tool agnostic functions. The trait can be mixed in SBT's or
  * Mill's native plugin constructs, which can then delegate to these functions,
  * keeping the build-tool-specific code down to a minimum.
  */
trait VcpkgPluginImpl {

  def vcpkgInstallImpl(
      dependencies: Set[String],
      manager: Vcpkg,
      logInfo: String => Unit
  ): Vector[Vcpkg.FilesInfo] = {
    val deps = dependencies.map(Vcpkg.Dependency.parse)

    val allActualDependencies = deps
      .flatMap { name =>
        val info = manager.dependencyInfo(name.name)
        val transitive = info.allTransitive(name)

        name +: transitive
      }
      .filterNot(_.name.startsWith("vcpkg-"))

    allActualDependencies.map { dep =>
      logInfo(s"Installing ${dep.name}")
      manager.install(dep.name)
      manager.files(dep.name)
    }.toVector
  }

  def vcpkgLinkingArgumentsImpl(
      info: Vector[Vcpkg.FilesInfo],
      logWarn: String => Unit
  ): Vector[String] = {
    val arguments = Vector.newBuilder[String]

    info.foreach { case f @ Vcpkg.FilesInfo(_, libDir) =>
      val static = f.staticLibraries
      val dynamic = f.dynamicLibraries

      if (dynamic.nonEmpty) {
        arguments += s"-L$libDir"
        dynamic.foreach { filePath =>
          val fileName = baseName(filePath)

          if (fileName.startsWith("lib"))
            arguments += "-l" + fileName.drop(3)
          else
            logWarn(
              s"Malformed dynamic library filename $fileName in $filePath"
            )
        }
      }

      static.foreach(f => arguments += f.toString)
    }
    arguments.result()
  }

  def vcpkgBinaryImpl(
      targetFolder: File,
      logInfo: String => Unit,
      logError: String => Unit
  ): File = {
    val destination = targetFolder / "vcpkg"

    val binary = destination / VcpkgBootstrap.BINARY_NAME
    val bootstrapScript = destination / VcpkgBootstrap.BOOTSTRAP_SCRIPT

    if (binary.exists) binary
    else if (bootstrapScript.exists) {
      logInfo("Bootstrapping vcpkg...")
      VcpkgBootstrap.launchBootstrap(destination, logError)

      binary
    } else {
      logInfo("Cloning and doing the whole shebang")
      VcpkgBootstrap.clone(destination)
      VcpkgBootstrap.launchBootstrap(destination, logError)

      binary
    }
  }

  def vcpkgCompilationArgumentsImpl(
      info: Vector[Vcpkg.FilesInfo]
  ): Vector[String] = {
    val arguments = Vector.newBuilder[String]

    info.foreach { case f @ Vcpkg.FilesInfo(includeDir, _) =>
      arguments += s"-I$includeDir"
    }

    arguments.result()
  }

  private def baseName(file: java.io.File): String = {
    val last = file.getName()
    val li = last.lastIndexOf('.')
    if (li == -1) last
    else last.slice(0, li)
  }

}
