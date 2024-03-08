package org.openrndr

import org.openrndr.platform.Platform

/**
 * Application configuration object
 */
object ApplicationConfiguration {
    val checkThread0 by lazy { Platform.property("org.openrndr.application.check_thread0")?.lowercase() != "false" }
    val restartJvmOnThread0 by lazy { Platform.property("org.openrndr.application.restart_jvm_on_thread0")?.lowercase() != "false" }
    val preloadClassName by lazy { Platform.property("org.openrndr.preloadclass") ?: "org.openrndr.Preload" }
}