package com.indoorvivants.vcpkg

import java.util.Arrays
import scala.sys.process
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors
import com.indoorvivants.vcpkg.Vcpkg.Logs.Buffer
import com.indoorvivants.vcpkg.Vcpkg.Logs.Redirect

import com.indoorvivants.detective.Platform

class Vcpkg(
    val config: Vcpkg.Configuration,
    logger: ExternalLogger
) {
  import sys.process.*
  import config.*
  private val localArg = s"--x-install-root=$installationDir"
  private val root = config.vcpkgRoot
  private val libDir = installationDir / vcpkgTriplet / "lib"

  private val pkgConfigDir = libDir / "pkgconfig"

  private lazy val vcpkgTriplet = config.vcpkgTriplet(Platform.target)

  private def cmd(args: String*) =
    Seq(binary.toString) ++ args ++ Seq(localArg)

  private def getLines(args: Seq[String]): Vector[String] = {
    import sys.process.Process
    val logs = Vcpkg.logCollector(
      out = Set(Vcpkg.Logs.Buffer, Vcpkg.Logs.Redirect(logger.debug)),
      err = Set(Vcpkg.Logs.Buffer, Vcpkg.Logs.Redirect(logger.debug))
    )
    val p = Process.apply(args, cwd = root).run(logs.logger).exitValue()

    if (p != 0) {
      logs.dump(logger.error)
      commandFailed(args, p)
    } else {
      logs.dump(logger.debug)
      logs.stdout()
    }
  }

  def dependencyInfo(name: String): Vcpkg.Dependencies =
    Vcpkg.Dependencies.parse(getLines(cmd("depend-info", name)))

  def install(name: String): Vector[String] =
    getLines(cmd("install", name, s"--triplet=$vcpkgTriplet", "--recurse"))

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
  ) {
    def vcpkgRoot: File = binary.getParentFile()
    def vcpkgTriplet(target: Platform.Target): String = {
      import Platform.Arch.*
      import Platform.OS.*
      import Platform.Bits

      import target.*

      val archPrefix = arch match {
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

      val osName = os match {
        case Linux   => "linux"
        case MacOS   => "osx"
        case Windows => "windows"
      }

      val lnk = (linking, os) match {
        case (Vcpkg.Linking.Static, Windows)        => Some("static")
        case (Vcpkg.Linking.Dynamic, Linux | MacOS) => Some("dynamic")
        case _                                      => None
      }

      (archPrefix :: osName :: lnk.toList).mkString("-")
    }

  }

  case class Dependency(name: String, features: List[String])
  object Dependency {
    def apply(name: String): Dependency = new Dependency(name, features = Nil)
    def parse(s: String): Dependency =
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
        if (l.toLowerCase.contains("a suitable version of cmake"))
          throw NoSuitableCmake
        else {
          l.split(": ", 2).toList match {
            case name :: deps :: Nil =>
              Dependency
                .parse(name) -> deps
                .split(", ")
                .toList
                .filterNot(_.trim.isEmpty)
                .map(Dependency.parse)
            case other => throw UnexpectedDependencyInfo(l)
          }
        }
      }.toMap)
    }
  }

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

  object Logs {
    sealed trait Collect extends Product with Serializable
    case object Buffer extends Collect
    case class Redirect(to: String => Unit) extends Collect
  }

  def logCollector(
      out: Set[Logs.Collect] = Set(Logs.Buffer),
      err: Set[Logs.Collect] = Set(Logs.Buffer)
  ) = {
    val stdout = Vector.newBuilder[String]
    val stderr = Vector.newBuilder[String]

    def handle(msg: String, c: Logs.Collect, buffer: String => Unit) =
      c match {
        case Buffer       => buffer(msg)
        case Redirect(to) => to(msg)
      }

    val logger = process.ProcessLogger.apply(
      (o: String) => {
        out.foreach { collector =>
          handle(o, collector, stdout.+=(_))

        }
      },
      (e: String) =>
        out.foreach { collector =>
          handle(e, collector, stderr.+=(_))
        }
    )

    Logs(logger, () => stdout.result(), () => stderr.result())
  }

}
