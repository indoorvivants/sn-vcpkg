package com.indoorvivants.vcpkg

import Dependency.parse

object DependencySpec extends weaver.FunSuite {
  test("basic parsing") {
    expect.all(
      parse("openssl") == Dependency("openssl"),
      parse("openssl[zlib,bla]") == Dependency("openssl", List("zlib", "bla")),
      parse("openssl[]") == Dependency("openssl"),
      parse("openssl[   zlib,    bla,test]") == Dependency(
        "openssl",
        List("zlib", "bla", "test")
      )
    )
  }

  test("short name") {
    expect.all(
      Dependency("openssl").short == "openssl",
      Dependency("openssl", List("zlib", "blib")).short == "openssl[zlib,blib]"
    )
  }
}
