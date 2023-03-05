package com.indoorvivants.vcpkg

import java.io.File
import com.indoorvivants.vcpkg.VcpkgDependencies.Names
import com.indoorvivants.vcpkg.VcpkgDependencies.ManifestFile
import java.io.FileReader

sealed trait VcpkgDependencies extends Product with Serializable {
  def dependencies(
      manifestReader: String => VcpkgManifestFile
  ): List[Dependency] =
    this match {
      case Names(deps) => deps
      case ManifestFile(path) =>
        val contents =
          io.Source.fromFile(path).getLines().mkString(System.lineSeparator())
        manifestReader(contents).dependencies.map {
          case Left(value)  => Dependency.parse(value)
          case Right(value) => value.toDependency
        }
    }
}

object VcpkgDependencies {
  case class Names(deps: List[Dependency]) extends VcpkgDependencies
  case class ManifestFile(path: File) extends VcpkgDependencies

  def apply(deps: String*): VcpkgDependencies = Names(
    deps.map(Dependency.parse(_)).toList
  )
  def apply(file: File): VcpkgDependencies = ManifestFile(file)
}

case class VcpkgManifestFile(
    name: String,
    dependencies: List[Either[String, VcpkgManifestDependency]]
)

case class VcpkgManifestDependency(
    name: String,
    features: List[String]
) {
  def toDependency = Dependency(name, features.toSet)
}
