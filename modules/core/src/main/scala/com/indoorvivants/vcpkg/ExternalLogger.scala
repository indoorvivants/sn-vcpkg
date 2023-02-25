package com.indoorvivants.vcpkg

case class ExternalLogger(
    debug: String => Unit,
    info: String => Unit,
    warn: String => Unit,
    error: String => Unit
)

object ExternalLogger {
  private val nopLog: String => Unit = _ => ()
  val nop = ExternalLogger(nopLog, nopLog, nopLog, nopLog)
}
