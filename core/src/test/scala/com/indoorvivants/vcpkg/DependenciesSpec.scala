package com.indoorvivants.vcpkg

import Dependencies.parse
import scala.util.Try

object DependenciesSpec extends weaver.FunSuite {
  test("detect cmake bootstrap") {
    val text =
      "A suitable version of cmake was not found (required v3.24.0). Downloading portable cmake v3.24.0..."

    expect(Try(parse(Vector(text))).toEither == Left(NoSuitableCmake))
  }

  test("full parse of depend-info command output") {

    val text = """
      | vcpkg-cmake: 
      | vcpkg-cmake-config: 
      | zlib: vcpkg-cmake
      | rocksdb[zlib]: vcpkg-cmake, vcpkg-cmake-config, zlib
      """.stripMargin.trim.linesIterator.toVector

    expect(
      parse(text) ==
        Dependencies(
          Map(
            Dependency("zlib") -> List(Dependency("vcpkg-cmake")),
            Dependency("vcpkg-cmake") -> Nil,
            Dependency("vcpkg-cmake-config") -> Nil,
            Dependency("rocksdb", List("zlib")) -> List(
              Dependency("vcpkg-cmake"),
              Dependency("vcpkg-cmake-config"),
              Dependency("zlib")
            )
          )
        )
    )
  }
}
