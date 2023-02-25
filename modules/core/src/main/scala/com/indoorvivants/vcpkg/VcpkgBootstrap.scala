package com.indoorvivants.vcpkg

import java.util.Arrays
import scala.sys.process
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors
import com.indoorvivants.detective.Platform
import Platform.OS._

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

  def launchBootstrap(directory: File, log: ExternalLogger) = {
    val script = directory / BOOTSTRAP_SCRIPT
    assert(
      script.exists(),
      s"vcpkg bootstrap script ($script) doesn't exist, did you forget to clone it?"
    )
    import sys.process._

    val collector = Logs.logCollector()

    val cmd = Seq(script.toString)

    val result = Process(cmd, cwd = directory).run(collector.logger).exitValue

    if (result != 0) collector.dump(log.error)

  }

  def manager(
      binary: File,
      installationDir: File,
      logger: ExternalLogger
  ) = {
    assert(
      binary.exists(),
      s"vcpkg executable ($binary) doesn't exist, did you forget to bootstrap it"
    )

    val config = Configuration(
      binary = binary,
      installationDir = installationDir,
      linking = Linking.Static
    )

    new Vcpkg(config, logger)
  }
}
