package com.indoorvivants.vcpkg

import com.indoorvivants.detective.Platform
import java.io.File

class VcpkgConfigurator(
    config: Vcpkg.Configuration,
    info: Map[Vcpkg.Dependency, Vcpkg.FilesInfo],
    logger: ExternalLogger
) {
  import config.*

  def dependencyFiles(name: Vcpkg.Dependency) =
    info(name)

  def dependencyIncludes(library: Vcpkg.Dependency) = {
    info(library).includeDir
  }

  def files(libraryName: String): Vcpkg.FilesInfo =
    info(Vcpkg.Dependency(libraryName))

  def includes(libraryName: String): File =
    info(Vcpkg.Dependency(libraryName)).includeDir

  def approximateLinkingArguments = {
    val arguments = Vector.newBuilder[String]

    sorted.foreach { case (name, f @ Vcpkg.FilesInfo(_, libDir)) =>
      val static = f.staticLibraries
      val dynamic = f.dynamicLibraries

      if (dynamic.nonEmpty) {
        arguments += s"-L$libDir"
        dynamic.foreach { filePath =>
          val fileName = baseName(filePath)

          if (fileName.startsWith("lib"))
            arguments += "-l" + fileName.drop(3)
          else
            logger.warn(
              s"Malformed dynamic library filename $fileName in $filePath"
            )
        }
      }

      static.foreach(f => arguments += f.toString)
    }
    arguments.result()
  }

  def approximateCompilationArguments = {
    val arguments = Vector.newBuilder[String]

    sorted.foreach { case (_, f @ Vcpkg.FilesInfo(includeDir, _)) =>
      arguments += s"-I$includeDir"
    }

    arguments.result()
  }

  def pkgConfig = new PkgConfig(pkgConfigDir, logger)

  private val sorted = info.toList.sortBy(_._1.name)
  private lazy val vcpkgTriplet = config.vcpkgTriplet(Platform.target)
  private val libDir = installationDir / vcpkgTriplet / "lib"

  private val pkgConfigDir = libDir / "pkgconfig"

  private def baseName(file: java.io.File): String = {
    val last = file.getName()
    val li = last.lastIndexOf('.')
    if (li == -1) last
    else last.slice(0, li)
  }
}
