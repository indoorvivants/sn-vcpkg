package com.indoorvivants

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths

package object vcpkg {
  private[vcpkg] implicit class FileOps(private val file: File) extends AnyVal {
    def /(s: String): File =
      file.toPath.resolve(s).toFile
  }

}
