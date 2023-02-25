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
    val vcpkgNativeLinkingArgs = taskKey[Seq[String]]("")
    val vcpkgNativeCompilingArgs = taskKey[Seq[String]]("")
    val vcpkgNativeAutoConfigure = settingKey[Boolean]("")
    val vcpkgNativeApproximate = settingKey[Boolean]("")
    val vcpkgNativeStaticLinking = settingKey[Boolean]("")
  }

  override def requires: Plugins = VcpkgPlugin && ScalaNativePlugin

  import autoImport._
  import VcpkgPlugin.{autoImport => VP}

  override lazy val projectSettings = Seq(
    vcpkgNativeAutoConfigure := true,
    vcpkgNativeApproximate := true,
    vcpkgNativeStaticLinking := false,
    vcpkgNativeLinkingArgs := linkingFlags(
      VP.vcpkgConfigurator.value,
      VP.vcpkgDependencies.value.toSeq.sorted,
      sbtLogger(sLog.value),
      vcpkgNativeApproximate.value
    ),
    vcpkgNativeCompilingArgs := compilationFlags(
      VP.vcpkgConfigurator.value,
      VP.vcpkgDependencies.value.toSeq.sorted,
      sbtLogger(sLog.value),
      vcpkgNativeApproximate.value
    ),
    ScalaNativePlugin.autoImport.nativeConfig := {
      if (vcpkgNativeAutoConfigure.value) {
        val conf = ScalaNativePlugin.autoImport.nativeConfig.value
        conf
          .withLinkingOptions(
            vcpkgNativeLinkingArgs.value ++ conf.linkingOptions
          )
          .withCompileOptions(
            vcpkgNativeCompilingArgs.value ++ conf.compileOptions
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
