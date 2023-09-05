package com.indoorvivants.vcpkg.millplugin.native

import utest._
import mill._
import mill.util.TestEvaluator
import mill.util.TestUtil
import com.indoorvivants.vcpkg._

object VcpkgNativeModuleSpec extends utest.TestSuite {

  val manifestPath =
    sys.env.get("MILL_VCPKG_ROOT").map { p =>
      os.Path(p) / "vcpkg.json"
    }

  def tests: Tests = Tests {
    test("base") {
      object build extends TestUtil.BaseModule {
        object foo extends VcpkgNativeModule {
          def vcpkgDependencies = T(VcpkgDependencies("cjson"))
          def scalaVersion = T("3.2.2")
          def scalaNativeVersion = T("0.4.15")

          override def vcpkgNativeConfig =
            T(super.vcpkgNativeConfig().addRenamedLibrary("cjson", "libcjson"))
        }
      }

      val eval = new TestEvaluator(build)
      val Right((result, _)) = eval(build.foo.vcpkgNativeLinking)

    }
  }
}
