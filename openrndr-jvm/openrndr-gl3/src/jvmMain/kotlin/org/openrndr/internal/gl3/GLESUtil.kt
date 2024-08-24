package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengles.*
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLES32
import org.lwjgl.system.APIUtil
import org.lwjgl.system.Callback
import org.lwjgl.system.MemoryUtil
import java.io.PrintStream
import java.util.*

private val logger = KotlinLogging.logger {}


/** OpenGL utilities.  */
object GLESUtil {
    /**
     * Detects the best debug output functionality to use and creates a callback that prints information to the specified [PrintStream]. The callback
     * function is returned as a [Callback], that should be [freed][Callback.free] when no longer needed.
     *
     * @param stream the output [PrintStream]
     */
    /**
     * Detects the best debug output functionality to use and creates a callback that prints information to [APIUtil.DEBUG_STREAM]. The callback
     * function is returned as a [Callback], that should be [freed][Callback.free] when no longer needed.
     */
    fun setupDebugMessageCallback() {
        val caps = GLES.getCapabilities()

        if (caps.GLES32) {
            logger.info { "Using GLES 3.2 for error logging." }
            val proc =
                GLDebugMessageCallback.create { source: Int, type: Int, id: Int, severity: Int, length: Int, message: Long, userParam: Long ->
                    val sb = StringBuilder(300)
                    sb.append("[LWJGL] OpenGL debug message\n")
                    printDetail(
                        sb,
                        "ID",
                        "0x" + Integer.toHexString(id).uppercase(Locale.getDefault())
                    )
                    printDetail(sb, "Source", getDebugSource(source))
                    printDetail(sb, "Type", getDebugType(type))
                    printDetail(sb, "Severity", getDebugSeverity(severity))
                    printDetail(
                        sb,
                        "Message",
                        GLDebugMessageCallback.getMessage(length, message)
                    )
                    logger.info { sb.toString() }
                }
            GLES32.glDebugMessageCallback(proc, MemoryUtil.NULL)
            //if ((glGetInteger(GLES32.GL_CONTEXT_FLAGS) and GLES32.GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
            logger.warn { "Warning: A non-debug context may not produce any debug output." }
            glEnable(GLES32.GL_DEBUG_OUTPUT)
            //}
            //return proc
        } else {
            logger.warn { "GLES debug not supported" }
        }
    }

    private fun printDetail(sb: StringBuilder, type: String, message: String) {
        sb
            .append("\t")
            .append(type)
            .append(": ")
            .append(message)
            .append("\n")
    }

    private fun getDebugSource(source: Int): String {
        return when (source) {
            GLES32.GL_DEBUG_SOURCE_API -> "API"
            GLES32.GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "WINDOW SYSTEM"
            GLES32.GL_DEBUG_SOURCE_SHADER_COMPILER -> "SHADER COMPILER"
            GLES32.GL_DEBUG_SOURCE_THIRD_PARTY -> "THIRD PARTY"
            GLES32.GL_DEBUG_SOURCE_APPLICATION -> "APPLICATION"
            GLES32.GL_DEBUG_SOURCE_OTHER -> "OTHER"
            else -> APIUtil.apiUnknownToken(source)
        }
    }

    private fun getDebugType(type: Int): String {
        return when (type) {
            GLES32.GL_DEBUG_TYPE_ERROR -> "ERROR"
            GLES32.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "DEPRECATED BEHAVIOR"
            GLES32.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "UNDEFINED BEHAVIOR"
            GLES32.GL_DEBUG_TYPE_PORTABILITY -> "PORTABILITY"
            GLES32.GL_DEBUG_TYPE_PERFORMANCE -> "PERFORMANCE"
            GLES32.GL_DEBUG_TYPE_OTHER -> "OTHER"
            GLES32.GL_DEBUG_TYPE_MARKER -> "MARKER"
            else -> APIUtil.apiUnknownToken(type)
        }
    }

    private fun getDebugSeverity(severity: Int): String {
        return when (severity) {
            GLES32.GL_DEBUG_SEVERITY_HIGH -> "HIGH"
            GLES32.GL_DEBUG_SEVERITY_MEDIUM -> "MEDIUM"
            GLES32.GL_DEBUG_SEVERITY_LOW -> "LOW"
            GLES32.GL_DEBUG_SEVERITY_NOTIFICATION -> "NOTIFICATION"
            else -> APIUtil.apiUnknownToken(severity)
        }
    }
}