package com.indoorvivants.vcpkg.millplugin

import utest.*
import mill.*
import mill.api.Discover
import mill.testkit.{TestRootModule, UnitTester}
import com.indoorvivants.vcpkg.*

object VcpkgModuleSpec extends utest.TestSuite {

  val vcpkgRoot = os.Path(
    sys.env
      .getOrElse("MILL_VCPKG_ROOT", sys.error("MILL_VCPKG_ROOT is not set"))
  )
  val manifestPath = vcpkgRoot / "vcpkg.json"

  object base extends TestRootModule {
    object foo extends VcpkgModule {
      def vcpkgDependencies = Task(VcpkgDependencies("cmark"))
    }
    lazy val millDiscover = Discover[this.type]
  }

  object `pkg-config` extends TestRootModule {
    object foo extends VcpkgModule {
      def vcpkgDependencies = Task(VcpkgDependencies(manifestPath.toIO))
    }
    lazy val millDiscover = Discover[this.type]
  }

  def tests: Tests = Tests {
    test("base") {

      UnitTester(base, vcpkgRoot / "resources" / "base").scoped { eval =>
        val Right(result) = eval(base.foo.vcpkgConfigurator): @unchecked
        assert(result.value.approximateCompilationArguments.size > 0)
        assert(result.value.approximateLinkingArguments.size > 0)
      }
    }

    test("pkg-config") {

      UnitTester(`pkg-config`, vcpkgRoot / "resources" / "pkg-config").scoped {
        eval =>
          val Right(result) =
            eval(`pkg-config`.foo.vcpkgConfigurator): @unchecked
          val Right(f) = eval(`pkg-config`.foo.vcpkgDependencies): @unchecked
          val pkgConfig = result.value.pkgConfig

          val includes =
            includePaths(pkgConfig.compilationFlags("libcmark", "libcjson"))

          val libNames =
            dynamicLibs(pkgConfig.linkingFlags("libcmark", "libcjson"))

          val paths = libPaths(pkgConfig.linkingFlags("libcmark", "libcjson"))

          assert(includes.exists(p => (p / "cmark.h").toIO.exists()))
          assert(includes.exists(p => (p / "cJSON.h").toIO.exists()))

          assert(
            paths.exists { p =>
              (p / FilesInfo.dynamicLibName("cmark")).toIO.exists() ||
              (p / FilesInfo.staticLibName("cmark")).toIO.exists() ||
              (p / FilesInfo.staticLibName("cmark_static")).toIO.exists()
            }
          )
          assert(
            paths.exists { p =>
              (p / FilesInfo.dynamicLibName("cjson")).toIO.exists() ||
              (p / FilesInfo.staticLibName("cjson")).toIO.exists()
            }
          )

          assert(libNames.contains("cmark"))
          assert(libNames.contains("cjson"))
      }
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
