package com.indoorvivants.vcpkg.millplugin

import upickle.default._
import com.indoorvivants.vcpkg.VcpkgNativeConfig

package object native {
  implicit val filesInfoRw: ReadWriter[VcpkgNativeConfig] =
    implicitly[ReadWriter[MVcpkgNativeConfig]]
      .bimap(MVcpkgNativeConfig.fromVcpkg, _.toVcpkg)

}
