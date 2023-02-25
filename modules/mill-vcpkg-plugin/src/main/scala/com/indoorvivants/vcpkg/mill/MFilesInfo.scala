package com.indoorvivants.vcpkg.millplugin

import mill._
import upickle.default._
import com.indoorvivants.vcpkg.Vcpkg
import com.indoorvivants.vcpkg._

private[vcpkg] case class MFilesInfo(includeDir: PathRef, libDir: PathRef) {
  def toVcpkgFilesInfo: FilesInfo =
    FilesInfo(includeDir.path.toIO, libDir.path.toIO)
}

private[vcpkg] object MFilesInfo {
  def fromVcpkgFilesInfo(filesInfo: FilesInfo): MFilesInfo =
    MFilesInfo(
      PathRef(os.Path(filesInfo.includeDir)),
      PathRef(os.Path(filesInfo.libDir))
    )

  implicit val readWriter: ReadWriter[MFilesInfo] = macroRW[MFilesInfo]
}
