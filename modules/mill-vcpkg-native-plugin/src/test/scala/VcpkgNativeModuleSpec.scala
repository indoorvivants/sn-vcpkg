package com.indoorvivants.vcpkg.millplugin.native

import utest.*
import mill.*
import mill.api.Discover
import mill.testkit.{TestRootModule, UnitTester}
import com.indoorvivants.vcpkg.*

object VcpkgNativeModuleSpec extends utest.TestSuite {

  val vcpkgRoot = os.Path(
    sys.env
      .getOrElse("MILL_VCPKG_ROOT", sys.error("MILL_VCPKG_ROOT is not set"))
  )
  val manifestPath = vcpkgRoot / "vcpkg.json"

  object base extends TestRootModule {
    object foo extends VcpkgNativeModule {
      def vcpkgDependencies = Task(VcpkgDependencies("cjson"))
      def scalaVersion = Task("3.2.2")
      def scalaNativeVersion = Task("0.4.15")

      override def vcpkgNativeConfig =
        Task(
          super.vcpkgNativeConfig().addRenamedLibrary("cjson", "libcjson")
        )
    }
    lazy val millDiscover = Discover[this.type]
  }

  def tests: Tests = Tests {
    test("base") {
      UnitTester(
        base,
        vcpkgRoot / "resources" / "base"
      ).scoped { eval =>
        val Right(result) = eval(base.foo.vcpkgNativeLinking): @unchecked
        assert(result.value.size > 0)
      }
    }
  }
}
