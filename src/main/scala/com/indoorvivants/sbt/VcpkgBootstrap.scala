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
  def clone(directory: File) = {
    import org.eclipse.jgit.api.Git

    val REMOTE_URI = "https://github.com/microsoft/vcpkg"

    val repo = Git
      .cloneRepository()
      .setURI(REMOTE_URI)
      .setDirectory(directory)
      .setBranchesToClone(Arrays.asList("refs/heads/master"))
      .setCloneAllBranches(false)
      .setCloneSubmodules(true)
      .call()

  }

  def launchBootstrap(directory: File) = {
    val script = directory / "bootstrap-vcpkg.sh"
    assert(
      script.exists(),
      s"vcpkg bootstrap script ($script) doesn't exist, did you forget to clone it?"
    )
    import sys.process.*

    val logger = Vcpkg.logCollector.logger

    val cmd = Seq(script.toString)

    assert(Process(cmd).run(logger).exitValue == 0)

  }

  def manager(binary: File, installationDir: File) = {
    assert(
      binary.exists(),
      s"vcpkg executable ($binary) doesn't exist, did you forget to bootstrap it"
    )

    new Vcpkg(binary, installationDir)
  }
}
