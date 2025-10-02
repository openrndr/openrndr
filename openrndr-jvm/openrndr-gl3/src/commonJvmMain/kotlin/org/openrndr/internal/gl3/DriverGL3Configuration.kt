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
     * Should a debug context be used, default is false
     */
    val useDebugContext by lazy { Platform.property("org.openrndr.gl3.debug") != null }

    /**
     * Should calling glfwTerminate be skipped on application shutdown? This is used to remedy problems during
     * testing. It looks like glfw can not be re-initialized after terminating it.
     * @since openrndr 0.4.5
     */
    val skipGlfwTermination by lazy { Platform.property("org.openrndr.gl3.skip_glfw_termination") != null }

    val glDriverTypeHint by lazy {
        when (Platform.property("org.openrndr.gl3.gl_type")) {
            "gl" -> DriverTypeGL.GL
            "gles" -> DriverTypeGL.GLES
            else -> null
        }
    }

    /**
     * Provided hints for selecting a GLES back-end.
     *
     * Hints can be provided by passing `-Dorg.openrndr.gl3.gles_backend=[angle|system]`
     * @since openrndr 0.4.5
     */
    val glesBackendHint by lazy {
        when (Platform.property("org.openrndr.gl3.gles_backend")) {
            "system" -> GlesBackend.SYSTEM
            "angle" -> {
                require(Platform.type == PlatformType.MAC) {
                    "Angle is only supported on macOS"
                }
                GlesBackend.ANGLE
            }
            else -> null
        }
    }

    /**
     * Determines which GLES back-end will be used
     * @see glesBackendHint
     * @since openrndr 0.4.5
     */
    val glesBackend by lazy {
        glesBackendHint ?: when (Pair(Platform.type, Platform.architecture)) {
            Pair(PlatformType.MAC, PlatformArchitecture.AARCH64) -> GlesBackend.ANGLE
            Pair(PlatformType.MAC, PlatformArchitecture.X86_64) -> GlesBackend.ANGLE
            else -> GlesBackend.SYSTEM
        }
    }

    /**
     * Determines which type of driver will be used
     * @see glDriverTypeHint
     * @since openrndr 0.4.5
     */

    val driverType by lazy {
        glDriverTypeHint ?: when (Pair(Platform.type, Platform.architecture)) {
            Pair(PlatformType.MAC, PlatformArchitecture.AARCH64) -> DriverTypeGL.GLES
            else -> DriverTypeGL.GL
        }
    }

    /**
     * Determines if the Angle runtime should be overwritten if one already exists, default is false.
     * @since openrndr 0.4.5
     */
    val overwriteExistingAngle by lazy {
        Platform.property("org.openrndr.gl3.overwrite_existing_angle") == "true"
    }


    /**
     * Determines if the Angle runtime should be deleted on exit, default is true.
     * @since openrndr 0.4.5
     */
    val deleteAngleOnExit by lazy {
        Platform.property("org.openrndr.gl3.delete_angle_on_exit") != "false"
    }

    val useBackBufferExtension by lazy {
        when (driverType) {
            DriverTypeGL.GL -> false
            DriverTypeGL.GLES -> false
        }
    }
}