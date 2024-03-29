package com.indoorvivants.vcpkg

import java.util.Arrays
import scala.sys.process
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors
import com.indoorvivants.vcpkg.Logs.Buffer
import com.indoorvivants.vcpkg.Logs.Redirect

import com.indoorvivants.detective.Platform

class Vcpkg(
    val config: Configuration,
    logger: ExternalLogger
) {
  import sys.process.*
  import config.*
  private def localArg = s"--x-install-root=$installationDir"
  private def rootArg = s"--vcpkg-root=$root"
  private def root = config.vcpkgRoot
  private def libDir = installationDir / vcpkgTriplet / "lib"

  private val pkgConfigDir = libDir / "pkgconfig"

  private lazy val vcpkgTriplet = config.vcpkgTriplet(Platform.target)

  private lazy val env = config.envVariables(Platform.target)

  private def cmd(args: String*) =
    Seq(binary.toString) ++ args ++ Seq(localArg, rootArg)

  private def cmdSeq(args: Seq[String]) =
    Seq(binary.toString) ++ args ++ Seq(localArg, rootArg)

  private def getLines(args: Seq[String], cwd: File = root): Vector[String] = {
    import sys.process.Process
    val logs = Logs.logCollector(
      out = Set(Logs.Buffer, Logs.Redirect(logger.debug)),
      err = Set(Logs.Buffer, Logs.Redirect(logger.debug))
    )
    logger.debug(s"Executing ${args.mkString("[", " ", "]")}")
    val p =
      Process.apply(args, cwd = cwd, env.toSeq *).run(logs.logger).exitValue()

    if (p != 0) {
      logs.dump(logger.error)
      commandFailed(args, p)
    } else {
      logs.dump(logger.debug)
      logs.stdout()
    }
  }

  def dependencyInfo(name: String): Dependencies =
    Dependencies.parse(getLines(cmd("depend-info", name)))

  def install(name: String): Vector[String] =
    getLines(cmd("install", name, s"--triplet=$vcpkgTriplet", "--recurse"))

  def installManifest(file: File): Vector[String] =
    getLines(
      cmd("install", s"--triplet=$vcpkgTriplet"),
      cwd = file.getParentFile()
    )

  def installAll(names: Seq[String]): Vector[String] =
    getLines(
      cmdSeq(
        Seq("install") ++ names ++ Seq(s"--triplet=$vcpkgTriplet", "--recurse")
      )
    )

  def list(): Vector[String] =
    getLines(cmd("list", s"--triplet=$vcpkgTriplet"))

  def pass(args: Seq[String]): Vector[String] =
    getLines(cmdSeq(args))

}
