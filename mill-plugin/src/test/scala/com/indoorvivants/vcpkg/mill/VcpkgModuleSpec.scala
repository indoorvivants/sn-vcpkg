package com.indoorvivants.vcpkg.mill

import utest._
import mill._
import mill.util.TestEvaluator
import mill.util.TestUtil
import com.indoorvivants.vcpkg.Vcpkg

object VcpkgModuleSpec extends utest.TestSuite {

  def tests: Tests = Tests {
    test("base") {
      object build extends TestUtil.BaseModule {
        object foo extends VcpkgModule {
          def vcpkgDependencies = T(Set("cmark"))
        }
      }

      val eval = new TestEvaluator(build)
      val Right((result, _)) = eval(build.foo.vcpkgConfigurator)
      assert(result.approximateCompilationArguments.size > 0)
    }

    test("pkg-config") {
      object build extends TestUtil.BaseModule {
        object foo extends VcpkgModule {
          def vcpkgDependencies = T(Set("cmark", "cjson"))
        }
      }

      val eval = new TestEvaluator(build)
      val Right((result, _)) = eval(build.foo.vcpkgConfigurator)
      val pkgConfig = result.pkgConfig

      val includes =
        includePaths(pkgConfig.compilationFlags("libcmark", "libcjson"))

      val libNames =
        dynamicLibs(pkgConfig.linkingFlags("libcmark", "libcjson"))

      val paths = libPaths(pkgConfig.linkingFlags("libcmark", "libcjson"))

      assert(includes.exists(p => (p / "cmark.h").toIO.exists()))
      assert(includes.exists(p => (p / "cJSON.h").toIO.exists()))

      assert(
        paths.exists { p =>
          (p / Vcpkg.FilesInfo.dynamicLibName("cmark")).toIO.exists() ||
          (p / Vcpkg.FilesInfo.staticLibName("cmark")).toIO.exists() ||
          (p / Vcpkg.FilesInfo.staticLibName("cmark_static")).toIO.exists()
        }
      )
      assert(
        paths.exists { p =>
          (p / Vcpkg.FilesInfo.dynamicLibName("cjson")).toIO.exists() ||
          (p / Vcpkg.FilesInfo.staticLibName("cjson")).toIO.exists()
        }
      )

      assert(libNames.contains("cmark"))
      assert(libNames.contains("cjson"))

    }
  }

  def includePaths(args: Seq[String]) =
    args.collect { case s"-I$path" =>
      os.Path(path)
    }

  def dynamicLibs(args: Seq[String]) =
    args.collect { case s"-l$name" => name }

  def libPaths(args: Seq[String]) =
    args.collect { case s"-L$path" => os.Path(path) }

}
