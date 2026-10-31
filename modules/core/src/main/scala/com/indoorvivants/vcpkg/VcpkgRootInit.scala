package com.indoorvivants.vcpkg

import java.io.File
import com.indoorvivants.detective.Platform
import dev.dirs.ProjectDirectories

trait VcpkgRootInit { self =>
  def locate(log: ExternalLogger): Either[String, VcpkgRoot]

  def orElse(other: VcpkgRootInit) = new VcpkgRootInit {
    private var logSelf = Option.empty[Boolean]
    private var logOther = Option.empty[Boolean]

    override def locate(log: ExternalLogger): Either[String, VcpkgRoot] = {
      val selfLocate = self.locate(log)
      logSelf = Some(selfLocate.isRight)

      lazy val otherLocate = other.locate(log)

      if (selfLocate.isRight) selfLocate
      else {
        logOther = Some(otherLocate.isRight)
        otherLocate
      }
    }

    override def toString(): String = {
      def render(s: Option[Boolean]) =
        s.map(if (_) "OK" else "SKIP").map("(" + _ + ")").getOrElse("")
      s"<$self${render(logSelf)} or else $other${render(logOther)}>"
    }

  }
}

case class VcpkgRoot(file: File, allowBootstrap: Boolean)

object VcpkgRootInit {
  class FromEnv private[vcpkg] (
      val name: String,
      val allowBootstrap: Boolean,
      val env: Map[String, String]
  ) extends VcpkgRootInit {
    override def locate(log: ExternalLogger): Either[String, VcpkgRoot] =
      env.get(name) match {
        case None => Left(s"Env Variable `$name` doesn't exist")
        case Some(value) =>
          val f = new File(value)
          if (!f.exists())
            Left(s"Folder `$value` from variable `$name` doesn't exist")
          else if (!f.isDirectory())
            Left(
              s"Location `$value` from variable `$name` is a file, not a folder"
            )
          else Right(VcpkgRoot(f, allowBootstrap))
      }

    override def toString(): String =
      s"VcpkgRootInit.FromEnv[name=$name,allowBootstrap=$allowBootstrap]"
  }

  object FromEnv {
    def apply(
        name: String = "VCPKG_ROOT",
        allowBootstrap: Boolean = true,
        env: Map[String, String] = sys.env
    ) =
      new FromEnv(name, allowBootstrap, env)
  }

  case class Manual(file: File, allowBootstrap: Boolean = true)
      extends VcpkgRootInit {
    override def locate(log: ExternalLogger): Either[String, VcpkgRoot] = {
      if (!file.exists())
        Left(s"Folder `$file` doesn't exist")
      else if (!file.isDirectory())
        Left(
          s"Location `$file` is a file, not a folder"
        )
      else Right(VcpkgRoot(file, allowBootstrap))
    }

    override def toString() =
      s"VcpkgRootInit.Manual[file=$file, allowBootstrap=$allowBootstrap]"
  }

  class SystemCache private[vcpkg] (
      val allowBootstrap: Boolean,
      val cacheDirDetector: CacheDirDetector
  ) extends VcpkgRootInit {

    private val cacheDir = cacheDirDetector.cacheDir

    override def locate(log: ExternalLogger): Either[String, VcpkgRoot] = Right(
      VcpkgRoot(cacheDir / "vcpkg", allowBootstrap)
    )

    override def toString(): String =
      s"VcpkgRootInit.SystemCache[allowBootstrap=$allowBootstrap,dir=$cacheDir]"
  }
  object SystemCache {
    def apply(allowBootstrap: Boolean = true) =
      new VcpkgRootInit.SystemCache(
        allowBootstrap,
        new CacheDirDetectorImpl(
          env = sys.env,
          props = sys.props.toMap,
          os = Platform.os
        )
      )
  }

}
