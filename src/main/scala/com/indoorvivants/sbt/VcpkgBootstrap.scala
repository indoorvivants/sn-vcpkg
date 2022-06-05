package com.indoorvivants.vcpkg.sbt

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import java.util.Arrays
import scala.sys.process
import java.nio.file.Files
import java.util.stream.Collectors
import com.indoorvivants.vcpkg.sbt.Platform.OS.Linux
import com.indoorvivants.vcpkg.sbt.Platform.OS.MacOS
import com.indoorvivants.vcpkg.sbt.Platform.OS.Unknown
import com.indoorvivants.vcpkg.sbt.Platform.OS.Windows

object VcpkgBootstrap {
  private val REMOTE_URI = "https://github.com/microsoft/vcpkg"

  lazy val BOOTSTRAP_SCRIPT =
    Platform.os match {
      case Windows => "bootstrap-vcpkg.bat"
      case _       => "bootstrap-vcpkg.sh"
    }

  lazy val BINARY_NAME =
    Platform.os match {
      case Windows => "vcpkg.exe"
      case _       => "vcpkg"
    }

  def clone(directory: File) = {
    import org.eclipse.jgit.api.Git

    val repo = Git
      .cloneRepository()
      .setURI(REMOTE_URI)
      .setDirectory(directory)
      .setBranchesToClone(Arrays.asList("refs/heads/master"))
      .setCloneAllBranches(false)
      .setCloneSubmodules(true)
      .call()

  }

  def launchBootstrap(directory: File, errorLogger: String => Unit) = {
    val script = directory / BOOTSTRAP_SCRIPT
    assert(
      script.exists(),
      s"vcpkg bootstrap script ($script) doesn't exist, did you forget to clone it?"
    )
    import sys.process.*

    val collector = Vcpkg.logCollector

    val cmd = Seq(script.toString)

    val result = Process(cmd).run(collector.logger).exitValue

    if (result != 0) collector.dump(errorLogger)

  }

  def manager(
      binary: File,
      installationDir: File,
      errorLogger: String => Unit
  ) = {
    assert(
      binary.exists(),
      s"vcpkg executable ($binary) doesn't exist, did you forget to bootstrap it"
    )

    new Vcpkg(binary, installationDir)
  }
}
