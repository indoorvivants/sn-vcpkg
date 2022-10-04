package com.indoorvivants.vcpkg

import java.nio.file.Files
import java.nio.file.attribute.FileAttribute
import java.io.File

import VcpkgRootInit._

object VcpkgRootInitSpec extends weaver.FunSuite {
  val logger = ExternalLogger.nop
  val dir = Files.createTempDirectory("vcpkg-testing-root-init").toFile

  dir.deleteOnExit()

  test("FromEnv checks that variable exists") {
    def fe(env: Map[String, String]) =
      new FromEnv(
        name = "HELLO_WORLD",
        allowBootstrap = true,
        env = env
      )

    expect.all(
      fe(Map.empty).locate(logger).isLeft,
      fe(Map("HELLO_WORLD" -> dir.toString)).locate(logger).isRight
    )
  }

  test("FromEnv uses VCPKG_ROOT by default") {
    expect.all(
      FromEnv().name == "VCPKG_ROOT"
    )
  }

  test("FromEnv checks that path exists and is a folder") {
    def fe(path: File) =
      new FromEnv(
        name = "VAR",
        allowBootstrap = true,
        env = Map("VAR" -> path.toString)
      )

    val tmpFile = Files.createTempFile(dir.toPath(), "howdy", "rowdy").toFile()
    tmpFile.deleteOnExit()

    expect.all(
      fe(dir).locate(logger).isRight,
      fe(dir / "bla").locate(logger).isLeft,
      fe(tmpFile).locate(logger).isLeft
    )
  }

  test("Manual checks that path exists and is a folder") {
    def man(path: File) =
      Manual(file = path)

    val tmpFile = Files.createTempFile(dir.toPath(), "howdy", "rowdy").toFile()
    tmpFile.deleteOnExit()

    expect.all(
      man(dir).locate(logger).isRight,
      man(dir / "bla").locate(logger).isLeft,
      man(tmpFile).locate(logger).isLeft
    )
  }

  test("SystemCache delegates to dirs-dev library") {
    val default =
      new CacheDirDetectorImpl(env = sys.env, props = sys.props.toMap)

    val systemCache = SystemCache()
    val stub = new CacheDirDetector {
      override def cacheDir: File = dir
    }

    expect.all(
      systemCache.locate(logger) == Right(
        VcpkgRoot(default.cacheDir / "vcpkg", true)
      ),
      new SystemCache(true, stub).locate(logger) == Right(
        VcpkgRoot(dir / "vcpkg", true)
      )
    )
  }

  test("orElse chooses the first working one") {
    val default =
      new CacheDirDetectorImpl(env = sys.env, props = sys.props.toMap)

    val chain1 =
      Manual(dir / "bla").orElse(Manual(dir / "fla")).orElse(Manual(dir))

    val chain2 =
      Manual(dir / "bla").orElse(Manual(dir)).orElse(Manual(dir / "fla"))

    val chain3 =
      Manual(dir / "bla")
        .orElse(FromEnv(name = "ASDSADASDASDSADASD"))
        .orElse(SystemCache())

    expect.all(
      chain1.locate(logger) == Right(VcpkgRoot(dir, true)),
      chain2.locate(logger) == Right(VcpkgRoot(dir, true)),
      chain3.locate(logger) == Right(
        VcpkgRoot(default.cacheDir / "vcpkg", true)
      )
    )
  }

}
