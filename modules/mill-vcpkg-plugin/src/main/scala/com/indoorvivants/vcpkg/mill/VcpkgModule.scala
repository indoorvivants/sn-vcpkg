package com.indoorvivants.vcpkg.millplugin

import com.indoorvivants.vcpkg.PkgConfig
import com.indoorvivants.detective.Platform.OS._
import com.indoorvivants.vcpkg
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
import mill.define.Task

trait VcpkgModule extends mill.define.Module with VcpkgPluginImpl {

  /** List of vcpkg dependencies
    */
  def vcpkgDependencies: T[vcpkg.VcpkgDependencies]

  /** Whether to bootstrap vcpkg automatically
    */
  def vcpkgRootInit: Task[vcpkg.VcpkgRootInit] = T.task { defaultRootInit }

  def vcpkgAllowBootstrap: T[Boolean] = T {
    vcpkgRootInit()
      .locate(millLogger(T.log))
      .map(_.allowBootstrap)
      .getOrElse(false)
  }

  def vcpkgRoot: T[os.Path] = T {
    val ioFile = vcpkgRootInit()
      .locate(millLogger(T.log))
      .map(_.file)
      .fold(
        msg =>
          throw new RuntimeException(
            s"Failed to identify a root for Vcpkg: `$msg`"
          ),
        identity
      )
    os.Path(ioFile)
  }

  def vcpkgInstallDir: T[os.Path] = {
    os.Path(defaultInstallDir)
  }

  def vcpkgConfigurator: Worker[VcpkgConfigurator] = T.worker {
    new VcpkgConfigurator(
      vcpkgManager().config,
      vcpkgInstall(),
      millLogger(T.log)
    )
  }

  /** "Path to vcpkg binary"
    */
  def vcpkgBinary: T[os.Path] = T {
    val ioFile = vcpkgBinaryImpl(
      root = vcpkg.VcpkgRoot(vcpkgRoot().toIO, vcpkgAllowBootstrap()),
      logger = millLogger(T.log)
    )
    os.Path(ioFile)
  }

  def vcpkgManager: Worker[vcpkg.Vcpkg] = T.worker {
    VcpkgBootstrap.manager(
      vcpkgBinary().toIO,
      vcpkgInstallDir().toIO,
      millLogger(T.log)
    )
  }

  def vcpkgInstall: T[Map[vcpkg.Dependency, vcpkg.FilesInfo]] = T {
    val manager = vcpkgManager()
    val logger = millLogger(T.log)

    vcpkgInstallImpl(
      vcpkgDependencies(),
      manager,
      logger
    )
  }

  protected def millLogger(log: Logger) = {
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
