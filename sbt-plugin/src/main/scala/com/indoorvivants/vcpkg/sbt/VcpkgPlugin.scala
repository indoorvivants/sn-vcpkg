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
    val vcpkgInstall = taskKey[Vector[Vcpkg.FilesInfo]]("")
    val vcpkgLinkingArguments = taskKey[Vector[String]]("")
    val vcpkgCompilationArguments = taskKey[Vector[String]]("")
    val vcpkgManager = taskKey[Vcpkg]("")
    val vcpkgConfigurator = taskKey[vcpkg.PkgConfig]("")
    val vcpkgBaseDir = taskKey[File]("")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    vcpkgBootstrap := true,
    vcpkgDependencies := Set.empty,
    vcpkgBaseDir := vcpkgDefaultBaseDir,
    vcpkgManager := {
      val binary = vcpkgBinary.value
      val installation = vcpkgBaseDir.value / "vcpkg-install"
      val logger = sLog.value
      val errorLogger = (s: String) => logger.error(s)
      val debugLogger = (s: String) => logger.debug(s)
      VcpkgBootstrap.manager(
        binary,
        installation,
        errorLogger = errorLogger,
        debugLogger = debugLogger
      )
    },
    vcpkgConfigurator := {
      val _ = vcpkgInstall.value

      vcpkgManager.value.pkgConfig
    },
    vcpkgBinary := {
      vcpkgBinaryImpl(
        targetFolder = vcpkgBaseDir.value,
        logInfo = sLog.value.info(_),
        logError = sLog.value.error(_)
      )
    },
    vcpkgInstall := {
      vcpkgInstallImpl(
        dependencies = vcpkgDependencies.value,
        manager = vcpkgManager.value,
        logInfo = sLog.value.info(_)
      ).toVector
    },
    vcpkgLinkingArguments := {
      vcpkgLinkingArgumentsImpl(
        info = vcpkgInstall.value,
        logWarn = sLog.value.error(_)
      )
    },
    vcpkgCompilationArguments := {
      vcpkgCompilationArgumentsImpl(
        info = vcpkgInstall.value
      )
    }
  )

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq()
}
