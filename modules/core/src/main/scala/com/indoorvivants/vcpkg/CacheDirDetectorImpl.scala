package com.indoorvivants.vcpkg

import java.io.File
import com.indoorvivants.detective.Platform
import dev.dirs.ProjectDirectories

private[vcpkg] class CacheDirDetectorImpl(
    env: Map[String, String] = sys.env,
    props: Map[String, String] = sys.props.toMap,
    os: Platform.OS = Platform.os
) extends CacheDirDetector {

  def cacheDir: File = {
    def absoluteFile(path: String): File = new File(path).getAbsoluteFile()
    def windowsCacheDirectory: File = {
      val base =
        env
          .get("LOCALAPPDATA")
          .map(absoluteFile)
          .getOrElse(
            absoluteFile(props("user.home")) / "AppData" / "Local"
          )

      base / "sbt-vcpkg" / "cache"
    }

    if (dirs.cacheDir.startsWith("null") && Platform.os == Platform.OS.Windows)
      windowsCacheDirectory
    else
      new File(dirs.cacheDir)
  }

  private def dirs: ProjectDirectories =
    dev.dirs.ProjectDirectories.fromPath("sbt-vcpkg")

}
