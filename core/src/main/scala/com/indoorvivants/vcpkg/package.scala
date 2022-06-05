package com.indoorvivants

import java.io.File

package object vcpkg {

  private val FILE_SEPARATOR = System.getProperty("file.separator");

  private[vcpkg] implicit class FileOps(private val file: File) extends AnyVal {
    def /(s: String): File = new File(file.getAbsolutePath() + FILE_SEPARATOR + s)
  }

}
