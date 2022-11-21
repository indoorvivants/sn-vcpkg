package com.indoorvivants.vcpkg
package cli

import cats.implicits.*
import com.monovore.decline.*
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

  private val vcpkgInstallDir = Opts
    .option[String](
      "vcpkg-install",
      metavar = "dir",
      help = "folder where packages will be installed"
    )
    .map(new File(_))
    .withDefault(defaultInstallDir)

  private val verbose = Opts
    .flag(
      long = "verbose",
      short = "v",
      visibility = Visibility.Normal,
      help = "Verbose logging"
    )
    .orFalse

  private val actionInstall =
    Opts
      .arguments[String](metavar = "dep")
      .map(_.toList)
      .map(Action.Install(_))

  private val actionInstallManifest =
    Opts
      .argument[String](
        "vcpkg manifest file"
      )
      .map(new File(_))
      .validate("File should exist")(f => f.exists() && f.isFile())
      .map(Action.InstallManifest(_))

  val logger = ExternalLogger(
    debug = scribe.debug(_),
    info = scribe.info(_),
    warn = scribe.warn(_),
    error = scribe.error(_)
  )

  case class Config(
      rootInit: VcpkgRootInit,
      installDir: File,
      allowBootstrap: Boolean,
      verbose: Boolean
  )

  private val configOpts =
    (vcpkgRootInit, vcpkgInstallDir, vcpkgAllowBootstrap, verbose).mapN(
      Config.apply
    )

  private val install =
    Opts.subcommand("install", "Install a list of vcpkg dependencies")(
      (actionInstall, configOpts).tupled
    )

  private val installManifest = Opts.subcommand(
    "install-manifest",
    "Install vcpkg dependencies from a manifest file (like vcpkg.json)"
  )(
    (actionInstallManifest, configOpts).tupled
  )

  val opts = Command(name, header)(install orElse installManifest)

end Options

object VcpkgCLI extends VcpkgPluginImpl:
  import Options.*

  def main(args: Array[String]): Unit =
    opts.parse(args) match
      case Left(help) =>
        System.err.println(help)
        if help.errors.nonEmpty then sys.exit(1)
        else sys.exit(0)
      case Right((action, config)) =>
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
            scribe.info(
              "Installed dependencies: ",
              vcpkgInstallImpl(dependencies.toSet, manager, logger)
                .map(_._1.name)
                .mkString(", ")
            )
          case Action.InstallManifest(file) =>
            scribe.info(
              "Installed dependencies: ",
              vcpkgInstallManifestImpl(file, manager, logger)
                .map(_._1.name)
                .mkString(", ")
            )
        end match
end VcpkgCLI
