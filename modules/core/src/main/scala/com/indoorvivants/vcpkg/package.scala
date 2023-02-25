package com.indoorvivants

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths

package object vcpkg {
  private[vcpkg] implicit class FileOps(private val file: File) extends AnyVal {
    def /(s: String): File =
      file.toPath.resolve(s).toFile
  }

  private[vcpkg] def commandFailed(
      args: Seq[String],
      code: Int,
      extraEnv: Map[String, String] = Map.empty
  ) = {
    val command = args.mkString("`", " ", "`")
    val env =
      if (extraEnv.nonEmpty)
        ", env: " + extraEnv.toSeq
          .sortBy(_._1)
          .map { case (k, v) => s"$k=$v" }
          .mkString(", ")
      else ""
    throw new Exception(s"Command $command failed with exit code $code$env")
  }

  private[vcpkg] object NoSuitableCmake
      extends RuntimeException(
        "Vcpkg couldn't find a suitable version of Cmake (this error is retryable)"
      )
  private[vcpkg] case class UnexpectedDependencyInfo(line: String)
      extends RuntimeException(
        s"Couldn't parse `${line.trim}` from vcpkg output as dependency information (this error is fatal)"
      )

}
