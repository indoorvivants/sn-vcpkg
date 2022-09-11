package com.indoorvivants.vcpkg

import java.io.File

import com.indoorvivants.detective.Platform

class PkgConfig(baseDir: File, logger: ExternalLogger) {

  lazy val binaryName = Platform.os match {
    case Platform.OS.Windows => "pkg-config.exe"
    case _                   => "pkg-config"
  }

  def compilationFlags(packages: String*): Seq[String] =
    updateCompilationFlags(Seq.empty, packages *)

  def linkingFlags(packages: String*): Seq[String] =
    updateLinkingFlags(Seq.empty, packages *)

  private val env = Map("PKG_CONFIG_PATH" -> baseDir.toString)

  private def getLines(args: Seq[String]) = {
    import sys.process.Process
    val logs = Vcpkg.logCollector()
    val p = Process
      .apply(args, cwd = None, extraEnv = env.toSeq *)
      .run(logs.logger)
      .exitValue()

    if (p != 0) {
      logs.dump(logger.error)
      logger.error(s"PkgConfig env: $env")
      commandFailed(args, p, env)
    } else {
      logs.dump(logger.debug)
      logs.stdout()
    }
  }

  def updateCompilationFlags(
      current: Seq[String],
      packages: String*
  ): Seq[String] = {
    val cmd = Seq(binaryName, "--cflags") ++ packages
    current ++ getLines(cmd)
      .flatMap(CommandParser.tokenize(_))
      .filterNot(current.contains)
  }

  def updateLinkingFlags(
      current: Seq[String],
      packages: String*
  ): Seq[String] = {
    val cmd = Seq(binaryName, "--libs") ++ packages
    current ++ getLines(cmd)
      .flatMap(CommandParser.tokenize(_))
      .filterNot(current.contains)
  }

}
