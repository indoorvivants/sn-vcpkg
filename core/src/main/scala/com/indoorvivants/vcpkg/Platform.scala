package com.indoorvivants.vcpkg

private[vcpkg] object Platform {
  sealed abstract class OS(val string: String) extends Product with Serializable
  object OS {
    case object Windows extends OS("windows")
    case object MacOS extends OS("osx")
    case object Linux extends OS("linux")
    case object Unknown extends OS("unknown")

    val all = List(Windows, MacOS, Linux, Unknown)
  }
  sealed abstract class Arch extends Product with Serializable {
    def string: String
  }
  object Arch {
    case object x86_64 extends Arch {
      override def string =
        os match {
          case OS.Windows => "x86"
          case _          => "x64"
        }
    }
    case object arm64 extends Arch {
      override def string = "arm64"
    }

    val all = List(x86_64, arm64)
  }

  case class Target(os: OS, arch: Arch) {
    def string = arch.string + "-" + os.string
    def fallback = (os, arch) match {
      case (OS.MacOS, Arch.arm64) => Some(Target(os, Arch.x86_64))
      case _                      => None
    }
  }

  def detectOs(osNameProp: String): OS = normalise(osNameProp) match {
    case p if p.startsWith("linux")                         => OS.Linux
    case p if p.startsWith("windows")                       => OS.Windows
    case p if p.startsWith("osx") || p.startsWith("macosx") => OS.MacOS
    case _                                                  => OS.Unknown
  }

  def detectArch(osArchProp: String): Arch = normalise(osArchProp) match {
    case "amd64" | "x64" | "x8664" | "x86" => Arch.x86_64
    case "aarch64" | "arm64"               => Arch.arm64
  }

  lazy val os = detectOs(sys.props.getOrElse("os.name", ""))
  lazy val arch = detectArch(sys.props.getOrElse("os.arch", ""))
  lazy val target = Target(os, arch)

  private def normalise(s: String) =
    s.toLowerCase(java.util.Locale.US).replaceAll("[^a-z0-9]+", "")

}
