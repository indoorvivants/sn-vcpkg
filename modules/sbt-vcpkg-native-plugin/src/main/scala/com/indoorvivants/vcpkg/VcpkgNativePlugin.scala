package com.indoorvivants.vcpkg.sbtplugin

import com.indoorvivants.vcpkg
import com.indoorvivants.detective.Platform.OS._
import com.indoorvivants.vcpkg.Vcpkg
import com.indoorvivants.vcpkg.VcpkgBootstrap
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import java.nio.file.Files
import java.util.Arrays
import java.util.stream.Collectors
import scala.sys.process
import sjsonnew.JsonFormat
import scala.util.control.NonFatal
import scala.scalanative.sbtplugin.ScalaNativePlugin
import scala.util.Try
import sjsonnew.support.scalajson.unsafe.Converter

object VcpkgNativePlugin extends AutoPlugin with vcpkg.VcpkgPluginNativeImpl {

  object autoImport {
    val vcpkgNativeLinking =
      taskKey[Seq[String]]("Configured linking flags for vcpkg packges")
    val vcpkgNativeCompilation =
      taskKey[Seq[String]]("Configured compilation flags for vcpkg packages")
    val vcpkgNativeConfig = settingKey[vcpkg.VcpkgNativeConfig](
      "Configuration object for VcpkgNativePlugin"
    )
  }

  override def requires: Plugins = VcpkgPlugin && ScalaNativePlugin

  import autoImport._
  import VcpkgPlugin.{autoImport => VP}

  private object manifestReader {
    import sjsonnew._, BasicJsonProtocol._
    implicit val depFormat = new JsonFormat[vcpkg.VcpkgManifestDependency] {
      def write[J](
          obj: vcpkg.VcpkgManifestDependency,
          builder: Builder[J]
      ): Unit = ???
      def read[J](
          jsOpt: Option[J],
          unbuilder: Unbuilder[J]
      ): vcpkg.VcpkgManifestDependency =
        jsOpt match {
          case Some(js) =>
            unbuilder.beginObject(js)
            val name = unbuilder.readField[String]("name")
            val features =
              Try(unbuilder.readField[List[String]]("features")).getOrElse(Nil)
            unbuilder.endObject()
            vcpkg.VcpkgManifestDependency(name, features)
          case None =>
            deserializationError("Expected JsObject but found None")
        }
    }

    implicit val fileFormat = new JsonReader[vcpkg.VcpkgManifestFile] {
      def read[J](
          jsOpt: Option[J],
          unbuilder: Unbuilder[J]
      ): vcpkg.VcpkgManifestFile =
        jsOpt match {
          case Some(js) =>
            unbuilder.beginObject(js)
            val name = unbuilder.readField[String]("name")
            val features = Try(
              unbuilder
                .readField[List[Either[String, vcpkg.VcpkgManifestDependency]]](
                  "dependencies"
                )
            ).getOrElse(Nil)
            unbuilder.endObject()
            vcpkg.VcpkgManifestFile(name, features)
          case None =>
            deserializationError("Expected JsObject but found None")
        }
    }

    def apply(content: String): vcpkg.VcpkgManifestFile = {
      import sjsonnew.support.scalajson.unsafe.Parser.parseUnsafe

      Converter
        .fromJson[vcpkg.VcpkgManifestFile](parseUnsafe(content))
        .fold(throw _, identity)
    }
  }

  override lazy val projectSettings = Seq(
    vcpkgNativeConfig := vcpkg.VcpkgNativeConfig(),
    vcpkgNativeLinking := linkingFlags(
      VP.vcpkgConfigurator.value,
      VP.vcpkgDependencies.value.dependencies(manifestReader(_)).map(_.short),
      sbtLogger(sLog.value),
      vcpkgNativeConfig.value
    ),
    vcpkgNativeCompilation := compilationFlags(
      VP.vcpkgConfigurator.value,
      VP.vcpkgDependencies.value.dependencies(manifestReader(_)).map(_.short),
      sbtLogger(sLog.value),
      vcpkgNativeConfig.value
    ),
    ScalaNativePlugin.autoImport.nativeConfig := {
      if (vcpkgNativeConfig.value.autoConfigure) {
        val conf = ScalaNativePlugin.autoImport.nativeConfig.value
        conf
          .withLinkingOptions(
            updateLinkingFlags(
              vcpkgNativeConfig.value,
              conf.linkingOptions,
              vcpkgNativeLinking.value
            )
          )
          .withCompileOptions(
            updateCompilationFlags(
              vcpkgNativeConfig.value,
              conf.compileOptions,
              vcpkgNativeCompilation.value
            )
          )
      } else ScalaNativePlugin.autoImport.nativeConfig.value
    }
  )

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq()

  private def sbtLogger(logger: sbt.Logger): vcpkg.ExternalLogger = {
    vcpkg.ExternalLogger(
      debug = logger.debug(_),
      info = logger.info(_),
      warn = logger.warn(_),
      error = logger.error(_)
    )

  }

}
