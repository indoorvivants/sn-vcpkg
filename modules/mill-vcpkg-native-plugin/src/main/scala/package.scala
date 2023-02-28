package com.indoorvivants.vcpkg.millplugin

import upickle.default._
import com.indoorvivants.vcpkg.VcpkgNativeConfig
import com.indoorvivants.vcpkg.VcpkgCodecs

package object native extends VcpkgCodecs {
  implicit val nativeConfRW: ReadWriter[VcpkgNativeConfig] =
    implicitly[ReadWriter[MVcpkgNativeConfig]]
      .bimap(MVcpkgNativeConfig.fromVcpkg, _.toVcpkg)

}
