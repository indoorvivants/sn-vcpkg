package com.indoorvivants.vcpkg.sbt

import com.indoorvivants.vcpkg
import com.indoorvivants.detective.Platform.OS._
import com.indoorvivants.vcpkg.Vcpkg
import com.indoorvivants.vcpkg.VcpkgBootstrap
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import java.nio.file.Files
import java.util.Arrays
import java.util.stream.Collectors
import scala.sys.process

object VcpkgPlugin extends AutoPlugin with vcpkg.VcpkgPluginImpl {

  object autoImport {
    val vcpkgDependencies =
      settingKey[Set[String]]("List of vcpkg dependencies")
    val vcpkgBootstrap =
      settingKey[Boolean]("whether to bootstrap vcpkg automatically")
    val vcpkgBinary = taskKey[File]("Path to vcpkg binary")
    val vcpkgInstall = taskKey[Map[Vcpkg.Dependency, Vcpkg.FilesInfo]](
      "Invoke Vcpkg and attempt to install packages"
    )
    val vcpkgManager = taskKey[Vcpkg]("")
    val vcpkgConfigurator = taskKey[vcpkg.VcpkgConfigurator]("")
    val vcpkgBaseDirectory = taskKey[File]("")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    vcpkgBootstrap := true,
    vcpkgDependencies := Set.empty,
    vcpkgBaseDirectory := vcpkgDefaultBaseDir,
    vcpkgManager := {
      val binary = vcpkgBinary.value
      val installation = vcpkgBaseDirectory.value / "vcpkg-install"

      VcpkgBootstrap.manager(
        binary,
        installation,
        logger = sbtLogger(sLog.value)
      )
    },
    vcpkgConfigurator := {
      val files = vcpkgInstall.value
      val manager = vcpkgManager.value

      new vcpkg.VcpkgConfigurator(
        manager.config,
        files,
        logger = sbtLogger(sLog.value)
      )
    },
    vcpkgBinary := {
      vcpkgBinaryImpl(
        targetFolder = vcpkgBaseDirectory.value,
        logInfo = sLog.value.info(_),
        logError = sLog.value.error(_)
      )
    },
    vcpkgInstall := {
      vcpkgInstallImpl(
        dependencies = vcpkgDependencies.value,
        manager = vcpkgManager.value,
        logger = sbtLogger(sLog.value)
      )
    }
  )

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq()

  private def sbtLogger(logger: sbt.Logger): vcpkg.ExternalLogger = {
    vcpkg.ExternalLogger(
      debug = logger.debug(_),
      info = logger.info(_),
      warn = logger.warn(_),
      error = logger.error(_)
    )
  }
}
