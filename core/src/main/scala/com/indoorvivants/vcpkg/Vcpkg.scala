package com.indoorvivants.vcpkg

import java.util.Arrays
import scala.sys.process
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors
import Platform.OS._

class Vcpkg(
    binary: File,
    installation: File,
    debug: String => Unit = _ => (),
    error: String => Unit = System.err.println
) {
  import sys.process.*
  private val localArg = s"--x-install-root=$installation"
  private val root = binary.getParentFile()

  private def cmd(args: String*) =
    Seq(binary.toString) ++ args ++ Seq(localArg)

  private def getLines(args: Seq[String]) = {
    import sys.process.Process
    val logs = Vcpkg.logCollector
    val p = Process.apply(args).run(logs.logger).exitValue()
    assert(p == 0)

    logs.dump(debug)

    logs.stdout()
  }

  def dependencyInfo(name: String) =
    Vcpkg.Dependencies.parse(getLines(cmd("depend-info", name)))

  def install(name: String) =
    getLines(cmd("install", name))

  def files(name: String) = {
    val triplet = Platform.target.string
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
