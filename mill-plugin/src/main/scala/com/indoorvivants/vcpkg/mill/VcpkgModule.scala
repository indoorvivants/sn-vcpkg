package com.indoorvivants.vcpkg.mill

import com.indoorvivants.vcpkg.PkgConfig
import com.indoorvivants.detective.Platform.OS._
import com.indoorvivants.vcpkg.Vcpkg
import com.indoorvivants.vcpkg.VcpkgBootstrap
import com.indoorvivants.vcpkg.VcpkgPluginImpl
import mill._
import mill.define.Discover
import mill.define.ExternalModule
import mill.define.Worker

import java.nio.file.Files
import java.util.Arrays
import java.util.stream.Collectors
import scala.sys.process

trait VcpkgModule extends mill.define.Module with VcpkgPluginImpl {

  /** List of vcpkg dependencies
    */
  def vcpkgDependencies: T[Set[String]]

  /** Whether to bootstrap vcpkg automatically
    */
  def vcpkgBootstrap: T[Boolean] = true

  def vcpkgBaseDir: T[os.Path] = os.Path(vcpkgDefaultBaseDir)

  def vcpkgConfigurator: Worker[PkgConfig] = T.worker {
    vcpkgInstall()
    vcpkgManager().pkgConfig
  }

  /** "Path to vcpkg binary"
    */
  def vcpkgBinary: T[os.Path] = T {
    val ioFile = vcpkgBinaryImpl(
      targetFolder = vcpkgBaseDir().toIO,
      logInfo = T.log.info(_),
      logError = T.log.error(_)
    )
    os.Path(ioFile)
  }

  def vcpkgManager: Worker[Vcpkg] = T.worker {
    val binary = vcpkgBinary().toIO
    val installation = vcpkgBaseDir() / "vcpkg-install"
    val errorLogger = (s: String) => T.log.errorStream.println(s)
    VcpkgBootstrap.manager(binary, installation.toIO, errorLogger)
  }

  def vcpkgInstall: T[Vector[Vcpkg.FilesInfo]] = T {
    vcpkgInstallImpl(
      dependencies = vcpkgDependencies(),
      manager = vcpkgManager(),
      logInfo = T.log.info(_: String)
    )
  }

  def vcpkgLinkingArguments: T[Vector[String]] = T {
    vcpkgLinkingArgumentsImpl(
      info = vcpkgInstall(),
      logWarn = T.log.error(_)
    )
  }

  def vcpkgCompilationArguments: T[Vector[String]] = T {
    vcpkgCompilationArgumentsImpl(
      info = vcpkgInstall()
    )
  }
}

object VcpkgModule extends ExternalModule with VcpkgPluginImpl {
  def millDiscover: Discover[this.type] = mill.define.Discover[this.type]
}
