package com.indoorvivants.vcpkg.millplugin

import com.indoorvivants.vcpkg.PkgConfig
import com.indoorvivants.detective.Platform.OS.*
import com.indoorvivants.vcpkg
import com.indoorvivants.vcpkg.ExternalLogger
import com.indoorvivants.vcpkg.VcpkgConfigurator
import com.indoorvivants.vcpkg.VcpkgBootstrap
import com.indoorvivants.vcpkg.VcpkgPluginImpl
import mill.*
import mill.api.Discover
import mill.api.ExternalModule
import mill.api.Logger

import java.nio.file.Files
import java.util.Arrays
import java.util.stream.Collectors
import scala.sys.process

trait VcpkgModule extends mill.api.Module with VcpkgPluginImpl {

  /** List of vcpkg dependencies
    */
  def vcpkgDependencies: T[vcpkg.VcpkgDependencies]

  /** Whether to bootstrap vcpkg automatically
    */
  def vcpkgRootInit: Task[vcpkg.VcpkgRootInit] = Task.Anon { defaultRootInit }

  def vcpkgAllowBootstrap: T[Boolean] = Task {
    vcpkgRootInit()
      .locate(millLogger(Task.log))
      .map(_.allowBootstrap)
      .getOrElse(false)
  }

  def vcpkgRoot: T[os.Path] = Task {
    val ioFile = vcpkgRootInit()
      .locate(millLogger(Task.log))
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

  def vcpkgConfigurator: Worker[VcpkgConfigurator] = Task.Worker {
    new VcpkgConfigurator(
      vcpkgManager().config,
      vcpkgInstall(),
      millLogger(Task.log)
    )
  }

  /** "Path to vcpkg binary"
    */
  def vcpkgBinary: T[os.Path] = Task {
    val ioFile = vcpkgBinaryImpl(
      root = vcpkg.VcpkgRoot(vcpkgRoot().toIO, vcpkgAllowBootstrap()),
      logger = millLogger(Task.log)
    )
    os.Path(ioFile)
  }

  def vcpkgManager: Worker[vcpkg.Vcpkg] = Task.Worker {
    VcpkgBootstrap.manager(
      vcpkgBinary().toIO,
      vcpkgInstallDir().toIO,
      millLogger(Task.log)
    )
  }

  def vcpkgInstall: T[Map[vcpkg.Dependency, vcpkg.FilesInfo]] = Task {
    val manager = vcpkgManager()
    val logger = millLogger(Task.log)

    vcpkgInstallImpl(
      vcpkgDependencies(),
      manager,
      logger
    )
  }

  def vcpkgRun(args: String*): Command[Unit] = Task.Command {
    val manager = vcpkgManager()
    val logger = millLogger(Task.log)

    vcpkgPassImpl(
      args,
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
  lazy val millDiscover = Discover[this.type]
}
