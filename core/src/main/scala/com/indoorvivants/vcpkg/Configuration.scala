package com.indoorvivants.vcpkg

import com.indoorvivants.detective.Platform
import java.io.File

case class Configuration(
    binary: File,
    installationDir: File,
    linking: Linking
) {
  def vcpkgRoot: File = binary.getParentFile()
  def vcpkgTriplet(target: Platform.Target): String = {
    import Platform.Arch.*
    import Platform.OS.*
    import Platform.Bits

    import target.*

    val archPrefix = arch match {
      case Intel =>
        Platform.bits match {
          case Bits.x32 => "x86"
          case Bits.x64 => "x64"
        }
      case Arm =>
        Platform.bits match {
          case Bits.x32 => "arm32"
          case Bits.x64 => "arm64"
        }
    }

    val osName = os match {
      case Linux   => "linux"
      case MacOS   => "osx"
      case Windows => "windows"
    }

    val lnk = (linking, os) match {
      case (Linking.Static, Windows)        => Some("static")
      case (Linking.Dynamic, Linux | MacOS) => Some("dynamic")
      case _                                => None
    }

    (archPrefix :: osName :: lnk.toList).mkString("-")
  }

}
