package com.indoorvivants.vcpkg

import java.util.Arrays
import scala.sys.process
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors
import Platform.OS._

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
    import sys.process._

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
