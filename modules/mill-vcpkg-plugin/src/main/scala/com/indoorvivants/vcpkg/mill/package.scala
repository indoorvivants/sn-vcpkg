package com.indoorvivants.vcpkg

import upickle.default._

package object millplugin {
  implicit val filesInfoRw: ReadWriter[FilesInfo] =
    implicitly[ReadWriter[MFilesInfo]]
      .bimap(MFilesInfo.fromVcpkgFilesInfo, _.toVcpkgFilesInfo)

  implicit val depRW: upickle.default.ReadWriter[Dependency] =
    upickle.default.macroRW[Dependency]
}
