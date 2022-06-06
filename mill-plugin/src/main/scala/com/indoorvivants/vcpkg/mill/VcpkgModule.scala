package com.indoorvivants.vcpkg.mill

import mill._
import java.util.Arrays
import scala.sys.process
import java.nio.file.Files
import java.util.stream.Collectors
import com.indoorvivants.vcpkg.Vcpkg
import com.indoorvivants.vcpkg.VcpkgBootstrap
import com.indoorvivants.vcpkg.Platform.OS._
import mill.define.Worker
import mill.define.ExternalModule
import mill.define.Discover
import com.indoorvivants.vcpkg.VcpkgPluginImpl

trait VcpkgModule extends mill.define.Module with VcpkgPluginImpl {

  /** List of vcpkg dependencies
    */
  def vcpkgDependencies: T[Set[String]]

  /** Whether to bootstrap vcpkg automatically
    */
  def vcpkgBootstrap: T[Boolean] = true

  def vcpkgManager: Worker[Vcpkg] = T.worker {
    val binary = VcpkgModule.vcpkgBinary().toIO
    val installation = T.dest / "vcpkg-install"
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

  /** "Path to vcpkg binary"
    */
  def vcpkgBinary: T[os.Path] = T {
    val ioFile = vcpkgBinaryImpl(
      targetFolder = T.dest.toIO,
      logInfo = T.log.info(_),
      logError = T.log.error(_)
    )
    os.Path(ioFile)
  }

  def millDiscover: Discover[this.type] = mill.define.Discover[this.type]
}
