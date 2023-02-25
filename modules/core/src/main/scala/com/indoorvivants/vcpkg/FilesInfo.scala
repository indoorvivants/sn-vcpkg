package com.indoorvivants.vcpkg

import com.indoorvivants.detective.Platform
import java.io.File
import java.util.stream.Collectors
import java.nio.file.Files

case class FilesInfo(includeDir: File, libDir: File) {
  import collection.JavaConverters._
  private def walk(
      path: java.nio.file.Path,
      predicate: java.nio.file.Path => Boolean
  ): Vector[File] =
    if (path.toFile().isDirectory)
      Files
        .walk(path)
        .filter(p => predicate(p))
        .collect(Collectors.toList())
        .asScala
        .toList
        .map(_.toFile)
        .toVector
    else Vector.empty

  def staticLibraries = {
    walk(
      libDir.toPath,
      _.getFileName.toString.endsWith(FilesInfo.staticLibExtension)
    )
  }

  def dynamicLibraries = {
    walk(
      libDir.toPath,
      _.getFileName.toString.endsWith(FilesInfo.dynamicLibExtension)
    )
  }

  def pkgConfigDir = libDir / "pkgconfig"
}

object FilesInfo {
  import Platform.OS._
  lazy val dynamicLibExtension = Platform.os match {
    case Linux | Unknown => ".so"
    case MacOS           => ".dylib"
    case Windows         => ".dll"
  }

  lazy val staticLibExtension = Platform.os match {
    case Windows => ".lib"
    case _       => ".a"
  }

  def dynamicLibName(name: String) = {
    val base = Platform.os match {
      case Linux | Unknown | MacOS => "lib" + name
      case Windows                 => name
    }

    base + dynamicLibExtension
  }

  def staticLibName(name: String) = {
    val base = Platform.os match {
      case Linux | Unknown | MacOS => "lib" + name
      case Windows                 => name
    }

    base + staticLibExtension
  }
}
