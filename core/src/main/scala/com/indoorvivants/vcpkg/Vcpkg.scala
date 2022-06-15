package com.indoorvivants.vcpkg

import java.util.Arrays
import scala.sys.process
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors
import Platform.OS._

class Vcpkg(
    config: Vcpkg.Configuration,
    debug: String => Unit = _ => (),
    error: String => Unit = System.err.println
) {
  import sys.process.*
  import config.*
  private val localArg = s"--x-install-root=$installationDir"
  private val root = binary.getParentFile()
  //   x64-linux
  // x64-windows
  // x64-windows-static
  // x86-windows
  // arm64-windows
  // x64-uwp
  // x64-osx
  // arm-uwp
  private val vcpkgTriplet = {
    import Platform.Arch.*
    import Platform.OS.*
    import Platform.Bits

    val archPrefix = Platform.arch match {
      case Intel =>
        Platform.bits match {
          case Bits.x32 => "x86"
          case Bits.x64 => "x64"
        }
      case Arm =>
        Platform.bits match {
          case Bits.x32 => "arm32"
          case Bits.x64 => "arm64"
        }
    }

    val os = Platform.os match {
      case Linux   => "linux"
      case MacOS   => "osx"
      case Windows => "windows"
    }

    val lnk = (linking, Platform.os) match {
      case (Vcpkg.Linking.Static, Windows)        => Some("static")
      case (Vcpkg.Linking.Dynamic, Linux | MacOS) => Some("dynamic")
      case _                                      => None
    }

    (archPrefix :: os :: lnk.toList).mkString("-")
  }

  private def commandFailed(args: Seq[String], code: Int) = {
    val command = args.mkString("`", " ", "`")
    throw new Exception(s"Command $command failed with exit code $code")
  }

  private def cmd(args: String*) =
    Seq(binary.toString) ++ args ++ Seq(localArg)

  private def getLines(args: Seq[String]) = {
    import sys.process.Process
    val logs = Vcpkg.logCollector
    val p = Process.apply(args, cwd = root).run(logs.logger).exitValue()

    if (p != 0) {
      logs.dump(error)
      commandFailed(args, p)
    } else {
      logs.dump(debug)
      logs.stdout()
    }
  }

  def dependencyInfo(name: String) =
    Vcpkg.Dependencies.parse(getLines(cmd("depend-info", name)))

  def install(name: String) =
    getLines(cmd("install", name, s"--triplet=$vcpkgTriplet"))

  def files(name: String) = {
    val triplet = vcpkgTriplet
    val installationName = name + "_" + triplet
    val location = root / "packages" / installationName

    Vcpkg.FilesInfo(
      includeDir = location / "include",
      libDir = location / "lib"
    )
  }

  def includes(library: String) = {
    files(library).includeDir
  }

}

object Vcpkg {
  sealed trait Linking
  object Linking {
    case object Static extends Linking
    case object Dynamic extends Linking
  }
  case class Configuration(
      binary: File,
      installationDir: File,
      linking: Linking
  )
  case class Dependency(name: String, features: List[String])
  object Dependency {
    def parse(s: String) =
      if (!s.contains('[')) Dependency(s, Nil)
      else {
        val start = s.indexOf('[')
        val end = s.indexOf(']')
        val features = s.substring(start + 1, end).split(", ").toList
        val name = s.take(start - 1)

        Dependency(name, features)
      }
  }

  case class Dependencies(packages: Map[Dependency, List[Dependency]]) {

    def allTransitive(name: Dependency): List[Dependency] = {
      def go(lib: Dependency): List[Dependency] =
        packages
          .collectFirst {
            case (dep, rest) if dep.name == lib.name => rest
          }
          .getOrElse(Nil)
          .flatMap { lb =>
            lb :: go(lb) // TODO: deal with circular dependencies?
          }
          .distinct

      go(name)
    }
  }
  object Dependencies {
    def parse(lines: Vector[String]): Dependencies = {
      Dependencies(lines.map { l =>
        val name :: deps :: Nil = l.split(": ", 2).toList
        Dependency
          .parse(name) -> deps
          .split(", ")
          .toList
          .filterNot(_.trim.isEmpty)
          .map(Dependency.parse)
      }.toMap)
    }
  }

  case class FilesInfo(includeDir: File, libDir: File) {
    import collection.JavaConverters._
    private def walk(
        path: java.nio.file.Path,
        predicate: java.nio.file.Path => Boolean
    ): Vector[File] =
      Files
        .walk(path)
        .filter(p => predicate(p))
        .collect(Collectors.toList())
        .asScala
        .toList
        .map(_.toFile)
        .toVector

    def staticLibraries = {
      val extension = Platform.os match {
        case Windows => ".lib"
        case _       => ".a"
      }
      walk(libDir.toPath, _.getFileName.toString.endsWith(extension))
    }

    def dynamicLibraries = {
      val extension = Platform.os match {
        case Linux   => ".so"
        case MacOS   => ".dylib"
        case Unknown => ".so"
        case Windows => ".dll"
      }
      walk(libDir.toPath, _.getFileName.toString.endsWith(extension))
    }

  }

  case class Logs(
      logger: process.ProcessLogger,
      stdout: () => Vector[String],
      stderr: () => Vector[String]
  ) {
    def dump(to: String => Unit) = {
      stdout().foreach(s => to(s"[vcpkg stdout] $s"))
      stderr().foreach(s => to(s"[vcpkg stderr] $s"))
    }
  }

  def logCollector = {
    val stdout = Vector.newBuilder[String]
    val stderr = Vector.newBuilder[String]

    val logger = process.ProcessLogger.apply(
      (o: String) => {
        stdout += o
      },
      (e: String) => stderr += e
    )

    Logs(logger, () => stdout.result(), () => stderr.result())
  }

}
