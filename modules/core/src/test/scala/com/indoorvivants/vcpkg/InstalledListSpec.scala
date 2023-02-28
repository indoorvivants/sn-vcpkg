package com.indoorvivants.vcpkg

import Dependencies.parse
import scala.util.Try

object InstalledListSpec extends weaver.FunSuite {

  test("basic parsing") {
    expect.same(
      InstalledList
        .parse(output.linesIterator.toVector, ExternalLogger.nop)
        .deps
        .sortBy(_.short),
      Vector(
        Dependency("libpq"),
        Dependency("libpq", Set("bonjour")),
        Dependency("libpq", Set("lz4")),
        Dependency("libpq", Set("openssl")),
        Dependency("libpq", Set("zlib")),
        Dependency("lz4"),
        Dependency("openssl"),
        Dependency("vcpkg-cmake"),
        Dependency("vcpkg-cmake-get-vars"),
        Dependency("vcpkg-cmake-config"),
        Dependency("zlib")
      ).sortBy(_.short)
    )
  }

  private val output = """
    |libpq:arm64-osx                                   14.4#1              The official database access API of postgresql
    |libpq[bonjour]:arm64-osx                                              Build with Bonjour support (--with-bonjour)
    |libpq[lz4]:arm64-osx                                                  Use lz4 (else --without-lz4)
    |libpq[openssl]:arm64-osx                                              support for encrypted client connections and ran...
    |libpq[zlib]:arm64-osx                                                 Use zlib (else --without-zlib)
    |lz4:arm64-osx                                     1.9.3#4             Lossless compression algorithm, providing compre...
    |openssl:arm64-osx                                 3.0.5#4             OpenSSL is an open source project that provides ...
    |vcpkg-cmake-config:arm64-osx                      2022-02-06#1        
    |vcpkg-cmake-get-vars:arm64-osx                    2022-05-10#1        
    |vcpkg-cmake:arm64-osx                             2022-08-18          
    |zlib:arm64-osx                                    1.2.12#2            A compression library
    """.stripMargin.trim
}
