package com.indoorvivants.vcpkg

case class Logs(
    logger: sys.process.ProcessLogger,
    stdout: () => Vector[String],
    stderr: () => Vector[String]
) {
  def dump(to: String => Unit) = {
    stdout().foreach(s => to(s"[vcpkg stdout] $s"))
    stderr().foreach(s => to(s"[vcpkg stderr] $s"))
  }
}

object Logs {
  sealed trait Collect extends Product with Serializable
  case object Buffer extends Collect
  case class Redirect(to: String => Unit) extends Collect

  def logCollector(
      out: Set[Logs.Collect] = Set(Logs.Buffer),
      err: Set[Logs.Collect] = Set(Logs.Buffer)
  ) = {
    val stdout = Vector.newBuilder[String]
    val stderr = Vector.newBuilder[String]

    def handle(msg: String, c: Logs.Collect, buffer: String => Unit) =
      c match {
        case Buffer       => buffer(msg)
        case Redirect(to) => to(msg)
      }

    val logger = sys.process.ProcessLogger.apply(
      (o: String) => {
        out.foreach { collector =>
          handle(o, collector, stdout.+=(_))

        }
      },
      (e: String) =>
        out.foreach { collector =>
          handle(e, collector, stderr.+=(_))
        }
    )

    Logs(logger, () => stdout.result(), () => stderr.result())
  }

}
