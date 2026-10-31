package com.indoorvivants.vcpkg.sbtplugin

import com.indoorvivants.vcpkg
import sjsonnew.JsonFormat
import sbt.util.CacheImplicits._

private[sbtplugin] trait JsonFormats {
  implicit val depFmt: JsonFormat[vcpkg.Dependency] =
    caseClass2(
      vcpkg.Dependency.apply,
      (d: vcpkg.Dependency) => Some((d.name, d.features))
    )(
      "name",
      "features"
    )

  implicit val fiFmt: JsonFormat[vcpkg.FilesInfo] =
    caseClass2(
      vcpkg.FilesInfo.apply,
      (f: vcpkg.FilesInfo) => Some((f.includeDir, f.libDir))
    )(
      "includeDir",
      "libDir"
    )
}
