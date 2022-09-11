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
import com.indoorvivants.vcpkg.ExternalLogger
import com.indoorvivants.vcpkg.VcpkgConfigurator
import mill.api.Logger

trait VcpkgModule extends mill.define.Module with VcpkgPluginImpl {

  /** List of vcpkg dependencies
    */
  def vcpkgDependencies: T[Set[String]]

  /** Whether to bootstrap vcpkg automatically
    */
  def vcpkgBootstrap: T[Boolean] = true

  def vcpkgBaseDirectory: T[os.Path] = os.Path(vcpkgDefaultBaseDir)

  def vcpkgConfigurator: Worker[VcpkgConfigurator] = T.worker {
    val files = vcpkgInstall()
    val manager = vcpkgManager()

    new VcpkgConfigurator(manager.config, files, millLogger(T.log))
  }

  /** "Path to vcpkg binary"
    */
  def vcpkgBinary: T[os.Path] = T {
    val ioFile = vcpkgBinaryImpl(
      targetFolder = vcpkgBaseDirectory().toIO,
      logInfo = T.log.info(_),
      logError = T.log.error(_)
    )
    os.Path(ioFile)
  }

  def vcpkgManager: Worker[Vcpkg] = T.worker {
    val binary = vcpkgBinary().toIO
    val installation = vcpkgBaseDirectory() / "vcpkg-install"
    VcpkgBootstrap.manager(binary, installation.toIO, millLogger(T.log))
  }

  def vcpkgInstall: T[Map[Vcpkg.Dependency, Vcpkg.FilesInfo]] = T {
    vcpkgInstallImpl(
      dependencies = vcpkgDependencies(),
      manager = vcpkgManager(),
      logger = millLogger(T.log)
    )
  }

  private def millLogger(log: Logger) = {
    new ExternalLogger(
      debug = log.debug(_),
      info = log.info(_),
      error = log.error(_),
      warn = s => log.info("<WARN>" + s)
    )
  }

}

object VcpkgModule extends ExternalModule with VcpkgPluginImpl {
  def millDiscover: Discover[this.type] = mill.define.Discover[this.type]
}
