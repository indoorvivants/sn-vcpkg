package com.indoorvivants.vcpkg

import upickle.default._
import mill.api.PathRef
import com.indoorvivants.vcpkg.VcpkgDependencies.ManifestFile
import com.indoorvivants.vcpkg.VcpkgDependencies.Names

private[vcpkg] trait VcpkgCodecs {
  implicit val filesInfoRw: ReadWriter[FilesInfo] =
    implicitly[ReadWriter[millplugin.MFilesInfo]]
      .bimap(millplugin.MFilesInfo.fromVcpkgFilesInfo, _.toVcpkgFilesInfo)

  implicit val depRW: upickle.default.ReadWriter[Dependency] =
    upickle.default.macroRW[Dependency]

  implicit val vcpkgDepsRW: upickle.default.ReadWriter[VcpkgDependencies] =
    implicitly[ReadWriter[Either[List[String], PathRef]]]
      .bimap[VcpkgDependencies](
        _ match {
          case ManifestFile(path) => Right(PathRef(os.Path(path)))
          case Names(deps)        => Left(deps.map(_.short))
        },
        _ match {
          case Left(value)  => VcpkgDependencies(value *)
          case Right(value) => VcpkgDependencies(value.path.toIO)
        }
      )
}

package object millplugin extends VcpkgCodecs
