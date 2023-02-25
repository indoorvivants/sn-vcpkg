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

object VcpkgNativePlugin extends AutoPlugin with vcpkg.VcpkgPluginNativeImpl {

  object autoImport {
    val vcpkgNativeLinking = taskKey[Seq[String]]("")
    val vcpkgNativeCompilation = taskKey[Seq[String]]("")
    val vcpkgNativeConfig = settingKey[vcpkg.VcpkgNativeConfig]("")
  }

  override def requires: Plugins = VcpkgPlugin && ScalaNativePlugin

  import autoImport._
  import VcpkgPlugin.{autoImport => VP}

  override lazy val projectSettings = Seq(
    vcpkgNativeConfig := vcpkg.VcpkgNativeConfig(),
    vcpkgNativeLinking := linkingFlags(
      VP.vcpkgConfigurator.value,
      VP.vcpkgDependencies.value.toSeq.sorted,
      sbtLogger(sLog.value),
      vcpkgNativeConfig.value
    ),
    vcpkgNativeCompilation := compilationFlags(
      VP.vcpkgConfigurator.value,
      VP.vcpkgDependencies.value.toSeq.sorted,
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
