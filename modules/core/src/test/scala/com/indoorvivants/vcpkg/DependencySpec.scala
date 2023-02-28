package com.indoorvivants.vcpkg

import Dependency.parse

object DependencySpec extends weaver.FunSuite {
  test("basic parsing") {
    expect.all(
      parse("openssl") == Dependency("openssl"),
      parse("openssl[zlib,bla]") == Dependency("openssl", Set("zlib", "bla")),
      parse("openssl[]") == Dependency("openssl"),
      parse("openssl[   zlib,    bla,test]") == Dependency(
        "openssl",
        Set("zlib", "bla", "test")
      )
    )
  }

  test("short name") {
    expect.all(
      Dependency("openssl").short == "openssl",
      Dependency("openssl", Set("zlib", "blib")).short == "openssl[blib,zlib]"
    )
  }
}
