package com.indoorvivants.vcpkg.sbtplugin

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
import sjsonnew.JsonFormat

object VcpkgPlugin extends AutoPlugin with vcpkg.VcpkgPluginImpl {

  object autoImport {
    val vcpkgDependencies =
      settingKey[Set[String]]("List of vcpkg dependencies")

    val vcpkgManifest =
      settingKey[File](
        "Path to the json file to be used as a vcpkg manifest. " +
          "If vcpkgDependencies is set and is non empty, then dependencies from the manifest" +
          " are installed first, and then the vcpkgDependencies"
      )

    val vcpkgInstall = taskKey[Map[vcpkg.Dependency, vcpkg.FilesInfo]](
      "Invoke Vcpkg and attempt to install packages"
    )

    val vcpkgRootInit = taskKey[vcpkg.VcpkgRootInit]("")
    val vcpkgRoot = taskKey[File]("Root of vcpkg installation")
    val vcpkgAllowBootstrap =
      taskKey[Boolean]("Allow bootstrapping root location")

    val vcpkgInstallDir = taskKey[File]("Where to put installed packages")

    val vcpkgBinary = taskKey[File]("Path to vcpkg binary")

    val vcpkgManager = taskKey[Vcpkg]("")

    val vcpkgConfigurator = taskKey[vcpkg.VcpkgConfigurator]("")
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
    vcpkgDependencies := Set.empty,
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
    vcpkgInstall := {
      val manager = vcpkgManager.value
      val logger = sbtLogger(sLog.value)
      val cacheFactory = streams.value.cacheStoreFactory
      val dependencies = vcpkgDependencies.value.toList.sorted

      val cachedInstall = Cache.cached(
        cacheFactory.make("sbt-vcpkg-vcpkg-dependencies-install")
      )((deps: List[String]) =>
        vcpkgInstallImpl(dependencies = deps.toSet, manager, logger).toList
      )

      val manifestFile = vcpkgManifest.?.value

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

      cachedManifestInstall(
        vcpkgManifest.?.value.map(f => f -> FileInfo.hash(f))
      )

      cachedInstall(dependencies).toMap
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
