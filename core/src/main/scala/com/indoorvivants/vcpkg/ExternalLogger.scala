package com.indoorvivants.vcpkg

case class ExternalLogger(
    debug: String => Unit,
    info: String => Unit,
    warn: String => Unit,
    error: String => Unit
)
