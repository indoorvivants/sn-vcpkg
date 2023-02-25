package com.indoorvivants.vcpkg

sealed trait Linking
object Linking {
  case object Static extends Linking
  case object Dynamic extends Linking
}
