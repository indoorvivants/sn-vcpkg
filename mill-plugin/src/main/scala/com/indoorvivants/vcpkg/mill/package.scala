package com.indoorvivants.vcpkg

import upickle.default._

package object mill {
  implicit val filesInfoRw: ReadWriter[Vcpkg.FilesInfo] =
    implicitly[ReadWriter[MFilesInfo]]
      .bimap(MFilesInfo.fromVcpkgFilesInfo, _.toVcpkgFilesInfo)

  implicit val depRW: upickle.default.ReadWriter[Vcpkg.Dependency] = upickle.default.macroRW[Vcpkg.Dependency]
}
