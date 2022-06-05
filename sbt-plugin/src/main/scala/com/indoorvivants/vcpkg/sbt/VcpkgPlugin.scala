package com.indoorvivants.vcpkg.sbt

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import java.util.Arrays
import scala.sys.process
import java.nio.file.Files
import java.util.stream.Collectors
import com.indoorvivants.vcpkg.Vcpkg
import com.indoorvivants.vcpkg.VcpkgBootstrap
import com.indoorvivants.vcpkg.Platform.OS._

object VcpkgPlugin extends AutoPlugin {

  object autoImport {
    val vcpkgDependencies =
      settingKey[Set[String]]("List of vcpkg dependencies")
    val vcpkgBootstrap =
      settingKey[Boolean]("whether to bootstrap vcpkg automatically")
    val vcpkgBinary = settingKey[File]("Path to vcpkg binary")
    val vcpkgInstall = settingKey[Vector[Vcpkg.FilesInfo]]("")
    val vcpkgLinkingArguments = settingKey[Vector[String]]("")
    val vcpkgCompilationArguments = settingKey[Vector[String]]("")
    val vcpkgManager = settingKey[Vcpkg]("")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    vcpkgBootstrap := true,
    vcpkgDependencies := Set.empty,
    vcpkgManager := {
      val binary = vcpkgBinary.value
      val installation = target.value / "vcpkg-install"
      val logger = sLog.value
      val errorLogger = (s: String) => logger.error(s)
      VcpkgBootstrap.manager(binary, installation, errorLogger)
    },
    vcpkgBinary := {
      val log = sLog.value
      val destination = target.value / "vcpkg"

      val binary = destination / VcpkgBootstrap.BINARY_NAME
      val bootstrapScript = destination / VcpkgBootstrap.BOOTSTRAP_SCRIPT
      val errorLogger = (s: String) => log.error(s)

      if (binary.exists()) binary
      else if (bootstrapScript.exists) {
        log.info("Bootstrapping vcpkg...")
        VcpkgBootstrap.launchBootstrap(destination, errorLogger)

        binary
      } else {
        log.info("Cloning and doing the whole shebang")
        VcpkgBootstrap.clone(destination)
        VcpkgBootstrap.launchBootstrap(destination, errorLogger)

        binary
      }

    },
    vcpkgInstall := {
      val deps = vcpkgDependencies.value.map(Vcpkg.Dependency.parse)
      val manager = vcpkgManager.value

      val log = sLog.value

      val allActualDependencies = deps
        .flatMap { name =>
          val info = manager.dependencyInfo(name.name)
          val transitive = info.allTransitive(name)

          name +: transitive
        }
        .filterNot(_.name.startsWith("vcpkg-"))

      allActualDependencies.map { dep =>
        log.info(s"Installing ${dep.name}")
        manager.install(dep.name)

        manager.files(dep.name)
      }.toVector
    },
    vcpkgLinkingArguments := {
      val log = sLog.value
      val info = vcpkgInstall.value
      val arguments = Vector.newBuilder[String]

      info.foreach { case f @ Vcpkg.FilesInfo(_, libDir) =>
        val static = f.staticLibraries
        val dynamic = f.dynamicLibraries

        if (dynamic.nonEmpty) {
          arguments += s"-L$libDir"
          dynamic.foreach { filePath =>
            val fileName = filePath.base

            if (fileName.startsWith("lib"))
              arguments += "-l" + fileName.drop(3)
            else
              log.warn(
                s"Malformed dynamic library filename $fileName in $filePath"
              )
          }
        }

        static.foreach(f => arguments += f.toString)
      }

      arguments.result()
    },
    vcpkgCompilationArguments := {
      val info = vcpkgInstall.value
      val arguments = Vector.newBuilder[String]

      info.foreach { case f @ Vcpkg.FilesInfo(includeDir, _) =>
        arguments += s"-I$includeDir"
      }

      arguments.result()
    }
  )

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq()
}
