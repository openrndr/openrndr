package org.openrndr.internal.gl3

import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformArchitecture
import org.openrndr.platform.PlatformType

/**
 * DriverGL3 configuration object
 * @since openrndr 0.4.5
 */
object DriverGL3Configuration {

    /**
     * Should a debug context be used
     */
    val useDebugContext by lazy { Platform.property("org.openrndr.gl3.debug") != null }

    val glDriverTypeHint by lazy {
        when (Platform.property("org.openrndr.gl3.gl_type")) {
            "gl" -> DriverTypeGL.GL
            "gles" -> DriverTypeGL.GLES
            else -> null
        }
    }

    val glesBackendHint by lazy {
        when (Platform.property("org.openrndr.gl3.gles_backend")) {
            "system" -> GlesBackend.SYSTEM
            "angle" -> {
                require(Platform.type == PlatformType.MAC && Platform.architecture == PlatformArchitecture.AARCH64) {
                    "Angle is only supported on macOS AArch64"
                }
                GlesBackend.ANGLE
            }

            else -> null
        }
    }

    val glesBackend by lazy {
        glesBackendHint ?: when (Pair(Platform.type, Platform.architecture)) {
            Pair(PlatformType.MAC, PlatformArchitecture.AARCH64) -> GlesBackend.ANGLE
            else -> GlesBackend.SYSTEM
        }
    }

    val driverType by lazy {
        glDriverTypeHint ?: when (Pair(Platform.type, Platform.architecture)) {
            Pair(PlatformType.MAC, PlatformArchitecture.AARCH64) -> DriverTypeGL.GLES
            else -> DriverTypeGL.GL
        }
    }

}