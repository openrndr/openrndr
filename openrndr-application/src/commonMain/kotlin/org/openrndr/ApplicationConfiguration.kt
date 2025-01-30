package org.openrndr

import org.openrndr.platform.Platform

/**
 * Application configuration object
 */
object ApplicationConfiguration {
    /**
     * Indicates whether the application should perform thread checking in certain operations.
     *
     * This variable is lazily initialized and its value is determined by reading the
     * `org.openrndr.application.check_thread0` property. If the property is explicitly set to
     * "false" (case-insensitive), the value will be `false`. Otherwise, the value defaults to `true`.
     */
    val checkThread0 by lazy { Platform.property("org.openrndr.application.check_thread0")?.lowercase() != "false" }

    /**
     * A lazily initialized property that determines whether the application should restart
     * the JVM on thread 0. This is configured via the system property
     * `org.openrndr.application.restart_jvm_on_thread0`.
     *
     * By default, this value is `true`, unless explicitly set to `"false"` (case-insensitive)
     * in the system properties.
     */
    val restartJvmOnThread0 by lazy { Platform.property("org.openrndr.application.restart_jvm_on_thread0")?.lowercase() != "false" }


    /**
     * The class name to be preloaded, determined lazily at runtime.
     * This variable fetches a system property `org.openrndr.preloadclass` using `Platform.property`.
     * If the property is not available, it defaults to `"org.openrndr.Preload"`.
     */
    val preloadClassName by lazy { Platform.property("org.openrndr.preloadclass") ?: "org.openrndr.Preload" }
}