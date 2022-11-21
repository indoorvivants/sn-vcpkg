package com.indoorvivants.vcpkg
package cli

import cats.implicits._
import com.monovore.decline._
import java.io.File

enum Action:
  case Install(dependencies: Seq[String])
  case InstallManifest(file: File)

object Options extends VcpkgPluginImpl:
  private val name = "scala-vcpkg"

  private val header = """
  |Bootstraps and installs vcpkg dependencies in a way compatible with 
  |the build tool plugins for SBT or Mill
  """.stripMargin.trim

  private val vcpkgAllowBootstrap =
    Opts
      .flag(
        "no-bootstrap",
        visibility = Visibility.Normal,
        help = "Allow bootstrapping vcpkg from scratch"
      )
      .orTrue

  private val envInit = Opts
    .option[String](
      "vcpkg-root-env",
      metavar = "env-var",
      visibility = Visibility.Normal,
      help = "Pick up vcpkg root from the environment variable"
    )
    .product(vcpkgAllowBootstrap)
    .map[VcpkgRootInit](VcpkgRootInit.FromEnv(_, _))

  private val manualInit = Opts
    .option[String](
      "vcpkg-root-manual",
      metavar = "location",
      visibility = Visibility.Normal,
      help = "Initialise vcpkg in this location"
    )
    .map(fname => new java.io.File(fname))
    .product(vcpkgAllowBootstrap)
    .map[VcpkgRootInit](VcpkgRootInit.Manual(_, _))

  private val vcpkgRootInit = manualInit
    .orElse(envInit)
    .orElse(
      vcpkgAllowBootstrap
        .map[VcpkgRootInit](allow => VcpkgRootInit.SystemCache(allow))
    )

  private val vcpkgInstallDir = Opts.option[String]("vcpkg-install", metavar = "dir", help = "folder where packages will be installed")
  .map(new File(_)).withDefault(defaultInstallDir)

  private val verbose = Opts.flag(long = "verbose", short = "v", visibility = Visibility.Normal, help = "Verbose logging").orFalse

  private val action = Opts.options[String]("install", help = "list of dependencies to install").map(_.toList).map(Action.Install(_)).orElse(
    Opts.option[String]("install-manifest", help = "install dependencies specified by the manifest file (e.g. vcpkg.json)", metavar = "file").map(new File(_)).validate("File should exist")(f => f.exists() && f.isFile()).map(Action.InstallManifest(_))
  )

  val logger = ExternalLogger(
    debug = scribe.debug(_),
    info = scribe.info(_),
    warn = scribe.warn(_),
    error = scribe.error(_)
  )

  case class Config(
    rootInit: VcpkgRootInit,
    installDir: File,
    action: Action,
    allowBootstrap: Boolean,
    verbose: Boolean
  )

  val opts = Command(name, header)((vcpkgRootInit, vcpkgInstallDir, action, vcpkgAllowBootstrap, verbose).mapN(Config.apply))



object VcpkgCLI extends VcpkgPluginImpl:
  import Options._
  
  def main(args: Array[String]): Unit = 
    opts.parse(args) match
      case Left(help) =>
        System.err.println(help)
        if help.errors.nonEmpty then
          sys.exit(1)
        else sys.exit(0)
      case Right(config) =>
        import config.*
        if verbose then 
          scribe.Logger.root.withMinimumLevel(scribe.Level.Trace).replace()
        val root = rootInit.locate(logger).fold(sys.error(_), identity)
        scribe.debug(s"Locating/bootstrapping vcpkg in ${root.file}")
        val binary = vcpkgBinaryImpl(root, logger)
        scribe.debug(s"Binary is $binary")
        val manager = VcpkgBootstrap.manager(binary, installDir, logger)

        action match
          case Action.Install(dependencies) => 
            scribe.info("Installed dependencies: ", 
              vcpkgInstallImpl(dependencies.toSet, manager, logger).map(_._1.name).mkString(", ")
              )
          case Action.InstallManifest(file) =>
            scribe.info("Installed dependencies: ", 
              vcpkgInstallManifestImpl(file, manager, logger).map(_._1.name).mkString(", ")
            )
        


