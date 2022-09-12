package com.indoorvivants.vcpkg

case class Dependency(name: String, features: List[String]) {
  val short = s"$name${features.mkString("[", ",", "]")}"
}
object Dependency {
  def apply(name: String): Dependency = new Dependency(name, features = Nil)
  def parse(s: String): Dependency =
    if (!s.contains('[')) Dependency(s, Nil)
    else {
      val start = s.indexOf('[')
      val end = s.indexOf(']')
      val features = s.substring(start + 1, end).split(", ").toList
      val name = s.take(start - 1)

      Dependency(name, features)
    }
}
