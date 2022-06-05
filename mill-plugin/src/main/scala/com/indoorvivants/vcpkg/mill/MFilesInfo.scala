package com.indoorvivants.vcpkg.mill

import mill._
import upickle.default._
import com.indoorvivants.vcpkg.Vcpkg

private[vcpkg] case class MFilesInfo(includeDir: PathRef, libDir: PathRef) {
  def toVcpkgFilesInfo: Vcpkg.FilesInfo =
    Vcpkg.FilesInfo(includeDir.path.toIO, libDir.path.toIO)
}

private[vcpkg] object MFilesInfo {
  def fromVcpkgFilesInfo(filesInfo: Vcpkg.FilesInfo): MFilesInfo =
    MFilesInfo(
      PathRef(os.Path(filesInfo.includeDir)),
      PathRef(os.Path(filesInfo.libDir))
    )

  implicit val readWriter: ReadWriter[MFilesInfo] = macroRW[MFilesInfo]
}
