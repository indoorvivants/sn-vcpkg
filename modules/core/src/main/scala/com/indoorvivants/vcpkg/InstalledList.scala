package com.indoorvivants.vcpkg

import scala.collection.immutable

class InstalledList private (val deps: Vector[Dependency])

object InstalledList {
  def parse(lines: Vector[String], logger: ExternalLogger) = {
    val parsed = lines.flatMap { line =>
      val dep = line.takeWhile(!_.isWhitespace).trim
      dep.split(":").toList match {
        case head :: next =>
          Some(Dependency.parse(head))
        case immutable.Nil =>
          logger.warn(s"Failed to parse `$dep` as installed dependency")

          None
      }

    }

    new InstalledList(parsed)
  }
}
