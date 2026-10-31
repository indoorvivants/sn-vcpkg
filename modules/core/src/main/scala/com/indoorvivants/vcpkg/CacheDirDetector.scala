package com.indoorvivants.vcpkg

import java.io.File
import com.indoorvivants.detective.Platform
import dev.dirs.ProjectDirectories

trait CacheDirDetector {
  def cacheDir: File
}

object CacheDirDetector {
  def apply() = new CacheDirDetectorImpl()
}
