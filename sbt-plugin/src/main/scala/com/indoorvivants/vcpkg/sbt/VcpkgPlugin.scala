package com.indoorvivants.vcpkg.sbt

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import java.util.Arrays
import scala.sys.process
import java.nio.file.Files
import java.util.stream.Collectors
import com.indoorvivants.vcpkg.Vcpkg
import com.indoorvivants.vcpkg.VcpkgBootstrap
import com.indoorvivants.vcpkg.Platform.OS._
import com.indoorvivants.vcpkg

object VcpkgPlugin extends AutoPlugin with vcpkg.VcpkgPluginImpl {

  object autoImport {
    val vcpkgDependencies =
      settingKey[Set[String]]("List of vcpkg dependencies")
    val vcpkgBootstrap =
      settingKey[Boolean]("whether to bootstrap vcpkg automatically")
    val vcpkgBinary = settingKey[File]("Path to vcpkg binary")
    val vcpkgInstall = settingKey[Vector[Vcpkg.FilesInfo]]("")
    val vcpkgLinkingArguments = settingKey[Vector[String]]("")
    val vcpkgCompilationArguments = settingKey[Vector[String]]("")
    val vcpkgManager = settingKey[Vcpkg]("")
    val vcpkgBaseDir = settingKey[File]("")
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
      // TODO: change back to debug
      val debugLogger = (s: String) => logger.info(s)
      VcpkgBootstrap.manager(
        binary,
        installation,
        errorLogger = errorLogger,
        debugLogger = debugLogger
      )
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
