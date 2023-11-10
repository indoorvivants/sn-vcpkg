package com.indoorvivants.vcpkg.sbtplugin

import com.indoorvivants.vcpkg
import com.indoorvivants.detective.Platform.OS._
import com.indoorvivants.vcpkg.{Vcpkg}
import com.indoorvivants.vcpkg.VcpkgBootstrap
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import java.nio.file.Files
import java.util.Arrays
import java.util.stream.Collectors
import scala.sys.process
import sjsonnew.JsonFormat

object VcpkgPlugin extends AutoPlugin with vcpkg.VcpkgPluginImpl {

  object autoImport {
    type VcpkgDependencies = vcpkg.VcpkgDependencies
    val VcpkgDependencies = vcpkg.VcpkgDependencies

    val vcpkgDependencies =
      settingKey[VcpkgDependencies]("List of vcpkg dependencies")

    val vcpkgInstall = taskKey[Map[vcpkg.Dependency, vcpkg.FilesInfo]](
      "Invoke Vcpkg and attempt to install packages"
    )

    val vcpkgRootInit = taskKey[vcpkg.VcpkgRootInit](
      "Specification of the location where vcpkg will be bootstrapped and where the packages will be installed"
    )
    val vcpkgRoot = taskKey[File]("Root of vcpkg installation")
    val vcpkgAllowBootstrap =
      taskKey[Boolean]("Allow bootstrapping root location")

    val vcpkgInstallDir = taskKey[File]("Where to put installed packages")

    val vcpkgBinary = taskKey[File]("Path to vcpkg binary")

    val vcpkgManager = taskKey[Vcpkg](
      "Value of `Vcpkg` type - a thin wrapper around the `vcpkg` CLI, already bootstrapped"
    )

    val vcpkgConfigurator = taskKey[vcpkg.VcpkgConfigurator](
      "`VcpkgConfigurator` instance, which provides access to PkgConfig (a thin wrapper over pre-configured pkg-config) and some methods to approximate compilation/linking arguments for installed packages"
    )

    val vcpkgRun = inputKey[Unit](
      "Run the bootstrapped vcpkg CLI with a given list of arguments"
    )
  }

  import autoImport._

  import sbt.util.CacheImplicits._

  implicit val depFmt: JsonFormat[vcpkg.Dependency] =
    caseClass2(vcpkg.Dependency.apply, vcpkg.Dependency.unapply)(
      "name",
      "features"
    )

  implicit val fiFmt: JsonFormat[vcpkg.FilesInfo] =
    caseClass2(vcpkg.FilesInfo.apply, vcpkg.FilesInfo.unapply)(
      "includeDir",
      "libDir"
    )

  override lazy val projectSettings = Seq(
    vcpkgDependencies := VcpkgDependencies(),
    vcpkgRootInit := defaultRootInit,
    vcpkgInstallDir := defaultInstallDir,
    vcpkgAllowBootstrap := vcpkgRootInit.value
      .locate(sbtLogger(sLog.value))
      .map(_.allowBootstrap)
      .fold(_ => false, identity),
    vcpkgRoot := vcpkgRootInit.value
      .locate(sbtLogger(sLog.value))
      .map(_.file)
      .fold(
        msg =>
          throw new MessageOnlyException(
            s"Failed to identify a root for Vcpkg: `$msg`"
          ),
        identity
      ),
    vcpkgManager := {
      VcpkgBootstrap.manager(
        vcpkgBinary.value,
        vcpkgInstallDir.value,
        logger = sbtLogger(sLog.value)
      )
    },
    vcpkgConfigurator := {
      new vcpkg.VcpkgConfigurator(
        vcpkgManager.value.config,
        vcpkgInstall.value,
        logger = sbtLogger(sLog.value)
      )
    },
    vcpkgBinary := {
      vcpkgBinaryImpl(
        root = vcpkg.VcpkgRoot(vcpkgRoot.value, vcpkgAllowBootstrap.value),
        logger = sbtLogger(sLog.value)
      )
    },
    vcpkgRun := {
      import complete.DefaultParsers._
      val args: Seq[String] = spaceDelimited("<arg>").parsed
      val manager = vcpkgManager.value
      val logger = sbtLogger(sLog.value)

      vcpkgPassImpl(args, manager, logger).foreach(println)

    },
    vcpkgInstall := {
      val manager = vcpkgManager.value
      val logger = sbtLogger(sLog.value)
      val cacheFactory = streams.value.cacheStoreFactory
      val dependencies = vcpkgDependencies.value

      val cachedInstall = Cache.cached(
        cacheFactory.make("sbt-vcpkg-vcpkg-dependencies-install")
      )((deps: List[vcpkg.Dependency]) =>
        vcpkgInstallDependenciesImpl(
          deps,
          manager,
          logger
        ).toList
      )

      type ManifestIn = Option[(File, HashFileInfo)]
      type StuffOut = List[(vcpkg.Dependency, vcpkg.FilesInfo)]

      val cachedManifestInstall =
        Tracked.lastOutput(cacheFactory.make("sbt-vcpkg-manifest-last")) {
          (min: ManifestIn, lastOutput: Option[StuffOut]) =>
            val f: ManifestIn => StuffOut =
              Tracked.inputChanged[ManifestIn, StuffOut](
                cacheFactory.make("sbt-vcpkg-input-changed")
              ) { (changed: Boolean, in: ManifestIn) =>
                if (changed) {
                  in.map(_._1)
                    .map { file =>
                      vcpkgInstallManifestImpl(file, manager, logger).toList
                    }
                    .getOrElse(Nil)
                } else lastOutput.getOrElse(List.empty)
              }

            f(min)
        }

      import VcpkgDependencies.*

      dependencies match {
        case ManifestFile(path) =>
          cachedManifestInstall(Some(path -> FileInfo.hash(path))).toMap
        case Names(deps) => cachedInstall(deps.toList).toMap
      }

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
