package com.indoorvivants.vcpkg
package cli

import cats.implicits.*
import com.monovore.decline.*
import java.io.File
import io.circe.Codec
import io.circe.CodecDerivation
import io.circe.Decoder

enum Compiler:
  case Clang, ClangPP

enum Action:
  case Install(dependencies: VcpkgDependencies, out: OutputOptions)
      extends Action
  case InvokeCompiler(
      compiler: Compiler,
      dependencies: VcpkgDependencies,
      args: Seq[String]
  ) extends Action
  case Bootstrap
end Action

case class SuspendedAction(apply: Seq[String] => Action)
object SuspendedAction:
  def immediate(action: Action) = SuspendedAction(_ => action)

case class OutputOptions(compile: Boolean, linking: Boolean)

object Options extends VcpkgPluginImpl:
  private val name = "sn-vcpkg"

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

  private val quiet = Opts
    .flag(
      long = "quiet",
      short = "q",
      visibility = Visibility.Normal,
      help = "Only error logging"
    )
    .orFalse

  private val outputCompile = Opts
    .flag(
      "output-compilation",
      short = "c",
      help =
        "Output (to STDOUT) compilation flags for installed libraries, one per line"
    )
    .orFalse

  private val outputLinking = Opts
    .flag(
      "output-linking",
      short = "l",
      help =
        "Output (to STDOUT) linking flags for installed libraries, one per line"
    )
    .orFalse

  private val out = (outputCompile, outputLinking).mapN(OutputOptions.apply)

  private val deps =
    Opts
      .option[String](
        "manifest",
        help = "vcpkg manifest file"
      )
      .map(new File(_))
      .validate("File should exist")(f => f.exists() && f.isFile())
      .map(VcpkgDependencies.apply)
      .orElse(
        Opts
          .arguments[String](metavar = "dep")
          .map(_.toList)
          .map(deps => VcpkgDependencies.apply(deps*))
      )

  private val actionInstall = (deps, out).mapN(Action.Install.apply)

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
      verbose: Boolean,
      quiet: Boolean
  )

  private val configOpts =
    (vcpkgRootInit, vcpkgInstallDir, vcpkgAllowBootstrap, verbose, quiet).mapN(
      Config.apply
    )

  private val install =
    Opts.subcommand("install", "Install a list of vcpkg dependencies")(
      (actionInstall.map(SuspendedAction.immediate), configOpts).tupled
    )

  private val bootstrap =
    Opts.subcommand("bootstrap", "Bootstrap vcpkg")(
      configOpts.map(SuspendedAction.immediate(Action.Bootstrap) -> _)
    )

  private def compilerCommand(compiler: Compiler) = deps.map(d =>
    SuspendedAction(rest => Action.InvokeCompiler(compiler, d, rest))
  )

  private val clang =
    Opts.subcommand(
      "clang",
      "Invoke clang with the correct flags for passed dependencies" +
        "The format of the command is [sn-vcpkg clang <flags> <dependencies> -- <clang arguments>"
    )(
      (compilerCommand(Compiler.Clang), configOpts).tupled
    )
  private val clangPP =
    Opts.subcommand(
      "clang++",
      "Invoke clang++ with the correct flags for passed dependencies. \n" +
        "The format of the command is [sn-vcpkg clang++ <flags> <dependencies> -- <clang arguments>"
    )(
      (compilerCommand(Compiler.ClangPP), configOpts).tupled
    )

  val opts =
    Command(name, header)(install orElse bootstrap orElse clang orElse clangPP)

end Options

object C:
  given cdc1: Decoder[VcpkgManifestDependency] =
    Decoder.instance { curs =>
      val name     = curs.get[String]("name")
      val features = curs.getOrElse[List[String]]("features")(Nil)

      import cats.syntax.all.*

      (name, features).mapN(VcpkgManifestDependency.apply)

    }

  given Decoder[Either[String, VcpkgManifestDependency]] =
    Decoder.decodeString
      .map(Left(_))
      .or(summon[Decoder[VcpkgManifestDependency]].map(Right(_)))

  given Decoder[VcpkgManifestFile] =
    Decoder.instance { curs =>
      val name = curs.get[String]("name")
      val deps = curs.getOrElse[List[Either[String, VcpkgManifestDependency]]](
        "dependencies"
      )(Nil)

      import cats.syntax.all.*

      (name, deps).mapN(VcpkgManifestFile.apply)

    }
end C

object VcpkgCLI extends VcpkgPluginImpl, VcpkgPluginNativeImpl:
  import Options.*

  def main(args: Array[String]): Unit =
    scribe.Logger.root
      .clearHandlers()
      .withHandler(writer = scribe.writer.SystemErrWriter)
      .replace()
    val arguments = args.takeWhile(_ != "--")
    val rest      = args.drop(arguments.length + 1)
    opts.parse(arguments) match
      case Left(help) =>
        val (modified, code) =
          if help.errors.nonEmpty then help.copy(body = Nil) -> -1
          else help                                          -> 0
        System.err.println(modified)
        if code != 0 then sys.exit(code)
      case Right((suspended, config)) =>
        val action = suspended.apply(rest)
        import config.*
        if verbose then
          scribe.Logger.root.withMinimumLevel(scribe.Level.Trace).replace()

        if quiet then scribe.Logger.root.clearHandlers().replace()

        val root = rootInit.locate(logger).fold(sys.error(_), identity)
        scribe.debug(s"Locating/bootstrapping vcpkg in ${root.file}")

        val binary = vcpkgBinaryImpl(root, logger)
        scribe.debug(s"Binary is $binary")

        val manager = VcpkgBootstrap.manager(binary, installDir, logger)
        def configurator(info: Map[Dependency, FilesInfo]) =
          VcpkgConfigurator(manager.config, info, logger)

        def summary(mp: Seq[Dependency]) =
          mp.map(_.name).toList.sorted.mkString(", ")

        def printOutput(
            opt: OutputOptions,
            deps: List[Dependency],
            info: Map[Dependency, FilesInfo]
        ) =
          val flags = List.newBuilder[String]

          if opt.compile then
            flags ++= compilationFlags(
              configurator(info),
              deps.map(_.short),
              logger,
              VcpkgNativeConfig()
            )
          if opt.linking then
            flags ++= linkingFlags(
              configurator(info),
              deps.map(_.short),
              logger,
              VcpkgNativeConfig()
            )

          flags.result().distinct
        end printOutput

        action match
          case Action.Bootstrap =>
            defaultRootInit.locate(logger) match
              case Left(value) => scribe.error(value)
              case Right(value) =>
                val bin = vcpkgBinaryImpl(value, logger)
                scribe.info(s"Vcpkg bootstrapped in $bin")
          case Action.Install(deps, output) =>
            val result = vcpkgInstallImpl(deps, manager, logger)

            import io.circe.parser.decode
            import C.given

            val allDeps =
              deps.dependencies(str =>
                decode[VcpkgManifestFile](str).fold(throw _, identity)
              )

            printOutput(
              output,
              allDeps,
              result.filter((k, v) => allDeps.contains(k))
            ).foreach(println)
          case Action.InvokeCompiler(compiler, deps, rest) =>
            val result = vcpkgInstallImpl(deps, manager, logger)
            import io.circe.parser.decode
            import C.given
            val allDeps =
              deps.dependencies(str =>
                decode[VcpkgManifestFile](str).fold(throw _, identity)
              )
            val compilerArgs = List.newBuilder[String]

            compilerArgs += (compiler match
              case Compiler.Clang   => "clang"
              case Compiler.ClangPP => "clang++"
            )

            compilerArgs.addAll(rest)

            printOutput(
              OutputOptions(compile = true, linking = true),
              allDeps,
              result.filter((k, v) => allDeps.contains(k))
            ).foreach(compilerArgs.addOne)


            scribe.debug(s"Invoking ${compiler} with arguments: [${compilerArgs.result().mkString(" ")}]")

            val exitCode = scala.sys.process.Process(compilerArgs.result()).!

            sys.exit(exitCode)

        end match
    end match
  end main

end VcpkgCLI
