version := "0.1"
scalaVersion := "3.2.0"

enablePlugins(VcpkgPlugin)

import com.indoorvivants.vcpkg

vcpkgDependencies := Set("cmark", "cjson")

val testPkgConfig = taskKey[Unit]("")

testPkgConfig := {
  val pkgConfig = vcpkgConfigurator.value.pkgConfig

  val compilation = pkgConfig.compilationFlags("libcmark", "libcjson")
  val linking = pkgConfig.linkingFlags("libcmark", "libcjson")

  val includes =
    includePaths(compilation)

  val libNames =
    dynamicLibs(linking)

  val paths = libPaths(linking)

  assert(includes.exists(p => (p / "cmark.h").exists()))
  assert(includes.exists(p => (p / "cJSON.h").exists()))

  assert(
    paths.exists { p =>
      (p / vcpkg.FilesInfo.dynamicLibName("cmark")).exists() ||
      (p / vcpkg.FilesInfo.staticLibName("cmark")).exists()
    }
  )
  assert(
    paths.exists { p =>
      (p / vcpkg.FilesInfo.dynamicLibName("cjson")).exists() ||
      (p / vcpkg.FilesInfo.staticLibName("cjson")).exists()
    }
  )

  assert(libNames.contains("cmark"))
  assert(libNames.contains("cjson"))
}

def includePaths(args: Seq[String]) =
  args.collect { case s if s.startsWith("-I") => new File(s.drop(2)) }

def dynamicLibs(args: Seq[String]) =
  args.collect { case s if s.startsWith("-l") => s.drop(2) }

def libPaths(args: Seq[String]) =
  args.collect { case s if s.startsWith("-L") => new File(s.drop(2)) }
