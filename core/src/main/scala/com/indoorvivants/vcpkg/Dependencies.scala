package com.indoorvivants.vcpkg

case class Dependencies(packages: Map[Dependency, List[Dependency]]) {

  def allTransitive(name: Dependency): List[Dependency] = {
    def go(lib: Dependency): List[Dependency] =
      packages
        .collectFirst {
          case (dep, rest) if dep.name == lib.name => rest
        }
        .getOrElse(Nil)
        .flatMap { lb =>
          lb :: go(lb) // TODO: deal with circular dependencies?
        }
        .distinct

    go(name)
  }
}
object Dependencies {
  def parse(lines: Vector[String]): Dependencies = {
    Dependencies(lines.map { l =>
      if (l.toLowerCase.contains("a suitable version of cmake"))
        throw NoSuitableCmake
      else {
        l.split(": ", 2).toList match {
          case name :: deps :: Nil =>
            Dependency
              .parse(name) -> deps
              .split(", ")
              .toList
              .filterNot(_.trim.isEmpty)
              .map(Dependency.parse)
          case other => throw UnexpectedDependencyInfo(l)
        }
      }
    }.toMap)
  }
}
