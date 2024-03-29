package com.indoorvivants.vcpkg

import com.indoorvivants.detective.Platform.OS._
import java.io.File
import scala.concurrent.duration.FiniteDuration
import scala.annotation.tailrec
import scala.util.control.NonFatal
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import com.indoorvivants.detective.Platform
import java.nio.file.Files
import com.indoorvivants.vcpkg.VcpkgDependencies.ManifestFile
import com.indoorvivants.vcpkg.VcpkgDependencies.Names

/** A bunch of build-tool agnostic functions. The trait can be mixed in SBT's or
  * Mill's native plugin constructs, which can then delegate to these functions,
  * keeping the build-tool-specific code down to a minimum.
  */
trait VcpkgPluginImpl {

  import scala.concurrent.duration.*

  protected def defaultRootInit =
    VcpkgRootInit.SystemCache(allowBootstrap = true)

  protected def defaultInstallDir =
    CacheDirDetector
      .apply()
      .cacheDir / "vcpkg-install"

  private def retryable[A](
      msg: String,
      maxAttempts: Int = 5,
      delay: FiniteDuration = 1.second,
      filter: Throwable => Boolean
  )(f: => A): Option[A] = {
    @tailrec def go(
        remainingAttempts: Int
    ): Option[A] = {
      if (remainingAttempts == 0) None
      else {
        if (maxAttempts != remainingAttempts) {
          val del =
            Math.pow(2.0, maxAttempts - remainingAttempts - 1) * delay.toMillis

          System.err.println(
            s"[sbt/mill vcpkg] Retrying `$msg` in $del millis ($remainingAttempts attempts left)..."
          )
          Thread.sleep(del.toLong)
        }

        Try(f) match {
          case Failure(exception) if filter(exception) =>
            go(remainingAttempts - 1)
          case Failure(exception) => throw exception
          case Success(value)     => Some(value)
        }

      }
    }

    go(maxAttempts)
  }

  protected def vcpkgInstallImpl(
      manifest: VcpkgDependencies,
      manager: Vcpkg,
      logger: ExternalLogger
  ) =
    manifest match {
      case ManifestFile(path) => vcpkgInstallManifestImpl(path, manager, logger)
      case Names(deps) =>
        vcpkgInstallDependenciesImpl(deps, manager, logger)
    }

  protected def vcpkgPassImpl(
      args: Seq[String],
      manager: Vcpkg,
      logger: ExternalLogger
  ): Vector[String] = manager.pass(args)

  protected def vcpkgInstallManifestImpl(
      manifest: File,
      manager: Vcpkg,
      logger: ExternalLogger
  ): Map[Dependency, FilesInfo] = {
    val tempDir = Files.createTempDirectory("vcpkg-manifest-install")
    val manifestFile =
      Files.copy(manifest.toPath, tempDir.resolve("vcpkg.json"))

    logger.debug(
      s"Installing dependencies from manifest file $manifest (using a working directory $tempDir)"
    )

    VcpkgPluginImpl.synchronized {
      manager.installManifest(manifestFile.toFile)

      InstalledList.parse(manager.list(), logger).deps.toSet

      InstalledList
        .parse(manager.list(), logger)
        .deps
        .map { dep =>
          dep -> files(dep.name, manager.config)
        }
        .toMap
    }
  }

  protected def vcpkgInstallDependenciesImpl(
      deps: List[Dependency],
      manager: Vcpkg,
      logger: ExternalLogger
  ): Map[Dependency, FilesInfo] = {

    val allActualDependencies = deps
      .flatMap { name =>
        val info =
          retryable(
            s"figuring out transitive dependencies of ${name.name}",
            filter = _ == NoSuitableCmake
          ) {
            VcpkgPluginImpl.synchronized(manager.dependencyInfo(name.name))
          }.getOrElse(
            throw new Exception(
              s"Failed to grab transitive dependencies of ${name.name}. Is vcpkg in a funk?"
            )
          )
        val transitive = info.allTransitive(name)

        name +: transitive
      }
      .filterNot(_.name.startsWith("vcpkg-"))

    VcpkgPluginImpl.synchronized {
      val allInstalledDependencies =
        InstalledList.parse(manager.list(), logger).deps.toSet

      val dependenciesToInstall =
        allActualDependencies.filterNot(allInstalledDependencies.contains(_))

      logger.debug(
        "Already installed dependencies: " + allInstalledDependencies
          .map(_.short)
          .toList
          .sorted
          .mkString(" ")
      )

      if (dependenciesToInstall.nonEmpty) {

        logger.info(
          "Installing: " + dependenciesToInstall
            .map(_.short)
            .toList
            .sorted
            .mkString(" ")
        )
        manager.installAll(dependenciesToInstall.map(_.short).toList)
      }

      InstalledList
        .parse(manager.list(), logger)
        .deps
        .map { dep =>
          dep -> files(dep.name, manager.config)
        }
        .toMap
    }

  }

  private def files(name: String, config: Configuration) = {
    val triplet = config.vcpkgTriplet(Platform.target)
    val installationName = name + "_" + triplet
    val location = config.vcpkgRoot / "packages" / installationName

    FilesInfo(
      includeDir = location / "include",
      libDir = location / "lib"
    )
  }

  protected def vcpkgBinaryImpl(
      root: VcpkgRoot,
      logger: ExternalLogger
  ): File = {
    val destination = root.file

    val binary = destination / VcpkgBootstrap.BINARY_NAME
    val bootstrapScript = destination / VcpkgBootstrap.BOOTSTRAP_SCRIPT

    if (root.allowBootstrap) {

      VcpkgPluginImpl.synchronized {

        if (binary.exists) binary
        else if (bootstrapScript.exists) {
          logger.info("Bootstrapping vcpkg...")
          VcpkgBootstrap.launchBootstrap(destination, logger)

          binary
        } else {
          logger.info(s"Cloning microsoft/vcpkg into $destination")
          VcpkgBootstrap.clone(destination, logger)
          VcpkgBootstrap.launchBootstrap(destination, logger)

          binary
        }
      }
    } else binary
  }

}

object VcpkgPluginImpl extends VcpkgPluginImpl
