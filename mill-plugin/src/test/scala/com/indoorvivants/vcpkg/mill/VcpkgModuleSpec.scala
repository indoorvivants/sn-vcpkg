package com.indoorvivants.vcpkg.mill

import utest._
import mill._
import mill.util.TestEvaluator
import mill.util.TestUtil

object VcpkgModuleSpec extends utest.TestSuite {

  def tests: Tests = Tests {
    test("foo") {
      object build extends TestUtil.BaseModule {
        object foo extends VcpkgModule {
          def vcpkgDependencies = T(Set("libuv"))
        }
      }

      val eval = new TestEvaluator(build)
      val Right((result, _)) = eval(build.foo.vcpkgCompilationArguments)
      assert(result.size > 0)
    }
  }

}
