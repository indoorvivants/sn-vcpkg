package com.indoorvivants.vcpkg.mill

import mill._
import java.util.Arrays
import scala.sys.process
import java.nio.file.Files
import java.util.stream.Collectors
import com.indoorvivants.vcpkg.Vcpkg
import com.indoorvivants.vcpkg.VcpkgBootstrap
import com.indoorvivants.vcpkg.Platform.OS._
import mill.define.Worker
import mill.define.ExternalModule
import mill.define.Discover

trait VcpkgModule extends mill.define.Module {

  /** List of vcpkg dependencies
    */
  def vcpkgDependencies: T[Set[String]]

  /** Whether to bootstrap vcpkg automatically
    */
  def vcpkgBootstrap: T[Boolean] = true

  def vcpkgManager: Worker[Vcpkg] = T.worker {
    val binary = VcpkgModule.vcpkgBinary().toIO
    val installation = T.dest / "vcpkg-install"
    val errorLogger = (s: String) => T.log.errorStream.println(s)
    VcpkgBootstrap.manager(binary, installation.toIO, errorLogger)
  }

  def vcpkgInstall: T[List[Vcpkg.FilesInfo]] = T {
    val deps = vcpkgDependencies().map(Vcpkg.Dependency.parse)
    val manager = vcpkgManager()

    val allActualDependencies = deps
      .flatMap { name =>
        val info = manager.dependencyInfo(name.name)
        val transitive = info.allTransitive(name)

        name +: transitive
      }
      .filterNot(_.name.startsWith("vcpkg-"))

    allActualDependencies.map { dep =>
      T.log.info(s"Installing ${dep.name}")
      manager.install(dep.name)

      manager.files(dep.name)
    }.toList
  }

  def vcpkgLinkingArguments: T[Vector[String]] = T {
    val info = vcpkgInstall()
    val arguments = Vector.newBuilder[String]

    info.foreach { case f @ Vcpkg.FilesInfo(_, libDir) =>
      val static = f.staticLibraries
      val dynamic = f.dynamicLibraries

      if (dynamic.nonEmpty) {
        arguments += s"-L$libDir"
        dynamic.foreach { filePath =>
          val fileName = os.Path(filePath).baseName

          if (fileName.startsWith("lib"))
            arguments += "-l" + fileName.drop(3)
          else
            T.log.error(
              s"Malformed dynamic library filename $fileName in $filePath"
            )
        }
      }

      static.foreach(f => arguments += f.toString)
    }

    arguments.result()
  }

  def vcpkgCompilationArguments: T[Vector[String]] = T {
    val info = vcpkgInstall()
    val arguments = Vector.newBuilder[String]

    info.foreach { case f @ Vcpkg.FilesInfo(includeDir, _) =>
      arguments += s"-I$includeDir"
    }

    arguments.result()
  }
}

object VcpkgModule extends ExternalModule {

  /** "Path to vcpkg binary"
    */
  def vcpkgBinary: T[os.Path] = T {
    val destination = T.dest / "vcpkg"

    val binary = destination / VcpkgBootstrap.BINARY_NAME
    val bootstrapScript = destination / VcpkgBootstrap.BOOTSTRAP_SCRIPT
    val errorLogger = (s: String) => T.log.error(s)

    if (os.exists(binary)) binary
    else if (os.exists(bootstrapScript)) {
      T.log.info("Bootstrapping vcpkg...")
      VcpkgBootstrap.launchBootstrap(destination.toIO, errorLogger)

      binary
    } else {
      T.log.info("Cloning and doing the whole shebang")
      VcpkgBootstrap.clone(destination.toIO)
      VcpkgBootstrap.launchBootstrap(destination.toIO, errorLogger)

      binary
    }
  }

  def millDiscover: Discover[this.type] = mill.define.Discover[this.type]
}
