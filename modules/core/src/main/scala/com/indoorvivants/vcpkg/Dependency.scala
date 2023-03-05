package com.indoorvivants.vcpkg

case class Dependency(name: String, features: Set[String]) {
  val short =
    s"$name${if (features.nonEmpty) features.toList.sorted.mkString("[", ",", "]") else ""}"
}
object Dependency {
  def apply(name: String): Dependency =
    new Dependency(name, features = Set.empty)
  def parse(s: String): Dependency =
    if (!s.contains('[')) Dependency(s, Set.empty)
    else {
      val start = s.indexOf('[')
      val end = s.indexOf(']')

      val features = s
        .substring(start + 1, end)
        .split(",")
        .map(_.trim)
        .filter(_.nonEmpty)
        .toSet

      val name = s.take(start)

      Dependency(name, features)
    }
}
