package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.egl.EGL
import org.lwjgl.egl.EGL10.*
import org.lwjgl.egl.EGL11.EGL_BACK_BUFFER
import org.lwjgl.egl.EGL11.EGL_TEXTURE_2D
import org.lwjgl.egl.EGL11.EGL_TEXTURE_FORMAT
import org.lwjgl.egl.EGL11.EGL_TEXTURE_RGBA
import org.lwjgl.egl.EGL11.EGL_TEXTURE_TARGET
import org.lwjgl.egl.EGL11.EGL_BIND_TO_TEXTURE_RGBA
import org.lwjgl.egl.EGL11.eglBindTexImage
import org.lwjgl.egl.EGL11.eglReleaseTexImage
import org.lwjgl.egl.EGL12.eglCreatePbufferFromClientBuffer
import org.lwjgl.opengles.GLES30
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.macosx.MacOSXLibrary
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.IOSurfaceImporter
import org.openrndr.internal.Driver
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import java.io.File

private val logger = KotlinLogging.logger { }

// EGL_ANGLE_iosurface_client_buffer
private const val EGL_IOSURFACE_ANGLE = 0x3454
private const val EGL_IOSURFACE_PLANE_ANGLE = 0x345A
private const val EGL_TEXTURE_TYPE_ANGLE = 0x345C
private const val EGL_TEXTURE_INTERNAL_FORMAT_ANGLE = 0x345D
private const val EGL_BIND_TO_TEXTURE_TARGET_ANGLE = 0x348D

// EGL_ANGLE_feature_control
private const val EGL_FEATURE_NAME_ANGLE = 0x3460
private const val EGL_FEATURE_STATUS_ANGLE = 0x3464
private const val EGL_FEATURE_COUNT_ANGLE = 0x3465

/** kCVPixelFormatType_32BGRA */
private const val PIXEL_FORMAT_BGRA = 0x42475241

/** kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange and FullRange, NV12 */
private const val PIXEL_FORMAT_420V = 0x34323076
private const val PIXEL_FORMAT_420F = 0x34323066

/** kCVPixelFormatType_420YpCbCr10BiPlanarVideoRange and FullRange, P010 */
private const val PIXEL_FORMAT_X420 = 0x78343230
private const val PIXEL_FORMAT_XF20 = 0x78663230

/**
 * The formats of an IOSurface plane, as EGL_ANGLE_iosurface_client_buffer and openrndr see them
 */
private class PlaneFormat(val internalFormat: Int, val type: Int, val format: ColorFormat, val colorType: ColorType)

private val BGRA8 = PlaneFormat(GL_BGRA, GL_UNSIGNED_BYTE, ColorFormat.BGRa, ColorType.UINT8)
private val R8 = PlaneFormat(GL_RED, GL_UNSIGNED_BYTE, ColorFormat.R, ColorType.UINT8)
private val RG8 = PlaneFormat(GL_RG, GL_UNSIGNED_BYTE, ColorFormat.RG, ColorType.UINT8)
private val R16 = PlaneFormat(GL_RED, GL_UNSIGNED_SHORT, ColorFormat.R, ColorType.UINT16)
private val RG16 = PlaneFormat(GL_RG, GL_UNSIGNED_SHORT, ColorFormat.RG, ColorType.UINT16)

private fun planeFormat(pixelFormat: Int, plane: Int): PlaneFormat? = when (pixelFormat) {
    PIXEL_FORMAT_BGRA -> if (plane == 0) BGRA8 else null
    PIXEL_FORMAT_420V, PIXEL_FORMAT_420F -> listOf(R8, RG8).getOrNull(plane)
    PIXEL_FORMAT_X420, PIXEL_FORMAT_XF20 -> listOf(R16, RG16).getOrNull(plane)
    else -> null
}

/** The ANGLE feature that makes IOSurface pbuffers sample the IOSurface instead of a copy */
private const val ZERO_COPY_FEATURE = "supportsMetalObjects"

/**
 * The functions of the IOSurface framework
 */
private object IOSurfaceFunctions {
    private val library = MacOSXLibrary.getWithIdentifier("com.apple.IOSurface")
    private val getWidthOfPlane = library.getFunctionAddress("IOSurfaceGetWidthOfPlane")
    private val getHeightOfPlane = library.getFunctionAddress("IOSurfaceGetHeightOfPlane")
    private val getPixelFormat = library.getFunctionAddress("IOSurfaceGetPixelFormat")

    fun width(ioSurface: Long, plane: Int) = JNI.invokePPP(ioSurface, plane.toLong(), getWidthOfPlane).toInt()
    fun height(ioSurface: Long, plane: Int) = JNI.invokePPP(ioSurface, plane.toLong(), getHeightOfPlane).toInt()
    fun pixelFormat(ioSurface: Long) = JNI.invokePI(ioSurface, getPixelFormat)
}

/**
 * Imports IOSurface planes through ANGLE's EGL_ANGLE_iosurface_client_buffer, for ANGLE builds that sample the
 * IOSurface itself
 */
internal class IOSurfaceImporterGL3 private constructor(
    private val display: Long,
    private val config: Long,
    private val maxImports: Int
) : IOSurfaceImporter {

    private class Import(val pbuffer: Long, val colorBuffer: ColorBufferGL3)

    private data class PlaneKey(val ioSurface: Long, val plane: Int)

    /** imports by IOSurface plane, in least recently used order */
    private val imports = LinkedHashMap<PlaneKey, Import>(16, 0.75f, true)

    private class PendingAction(val sync: Long, val action: () -> Unit)

    private val pendingActions = ArrayDeque<PendingAction>()

    private var unsupportedFormatLogged = false

    override fun import(ioSurface: Long, plane: Int): ColorBuffer? {
        val key = PlaneKey(ioSurface, plane)
        imports[key]?.let { return it.colorBuffer }

        val pixelFormat = IOSurfaceFunctions.pixelFormat(ioSurface)
        val planeFormat = planeFormat(pixelFormat, plane)
        if (planeFormat == null) {
            if (!unsupportedFormatLogged) {
                logger.warn { "can not import plane $plane of IOSurface with pixel format 0x${pixelFormat.toString(16)}" }
                unsupportedFormatLogged = true
            }
            return null
        }
        val width = IOSurfaceFunctions.width(ioSurface, plane)
        val height = IOSurfaceFunctions.height(ioSurface, plane)

        val attributes = intArrayOf(
            EGL_WIDTH, width,
            EGL_HEIGHT, height,
            EGL_IOSURFACE_PLANE_ANGLE, plane,
            EGL_TEXTURE_TARGET, EGL_TEXTURE_2D,
            EGL_TEXTURE_INTERNAL_FORMAT_ANGLE, planeFormat.internalFormat,
            EGL_TEXTURE_FORMAT, EGL_TEXTURE_RGBA,
            EGL_TEXTURE_TYPE_ANGLE, planeFormat.type,
            EGL_NONE
        )
        val pbuffer = eglCreatePbufferFromClientBuffer(display, EGL_IOSURFACE_ANGLE, ioSurface, config, attributes)
        if (pbuffer == EGL_NO_SURFACE) {
            logger.error { "failed to create pbuffer for IOSurface: EGL error 0x${eglGetError().toString(16)}" }
            return null
        }

        val previousTexture = IntArray(1)
        glGetIntegerv(GL_TEXTURE_BINDING_2D, previousTexture)
        val texture = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        val bound = eglBindTexImage(display, pbuffer, EGL_BACK_BUFFER)
        glBindTexture(GL_TEXTURE_2D, previousTexture[0])

        if (!bound) {
            logger.error { "failed to bind IOSurface pbuffer: EGL error 0x${eglGetError().toString(16)}" }
            glDeleteTextures(texture)
            eglDestroySurface(display, pbuffer)
            return null
        }

        val colorBuffer = ColorBufferGL3(
            DriverGL3.generateResourceId(), GL_TEXTURE_2D, texture, width, height, 1.0,
            planeFormat.format, planeFormat.colorType, 1, BufferMultisample.Disabled, null
        )
        imports[key] = Import(pbuffer, colorBuffer)

        if (imports.size > maxImports) {
            val (leastRecentlyUsed, import) = imports.entries.first()
            imports.remove(leastRecentlyUsed)
            destroy(import)
        }
        return colorBuffer
    }

    override fun runWhenGpuFinished(action: () -> Unit) {
        val sync = GLES30.glFenceSync(GLES30.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
        pendingActions.addLast(PendingAction(sync, action))
    }

    override fun update() {
        // fences are signaled in order
        while (pendingActions.isNotEmpty()) {
            val status = GLES30.glClientWaitSync(pendingActions.first().sync, GLES30.GL_SYNC_FLUSH_COMMANDS_BIT, 0L)
            if (status == GLES30.GL_TIMEOUT_EXPIRED) {
                break
            }
            if (status == GLES30.GL_WAIT_FAILED) {
                logger.error { "waiting for fence failed" }
            }
            runFirstPendingAction()
        }
    }

    private fun runFirstPendingAction() {
        val pending = pendingActions.removeFirst()
        GLES30.glDeleteSync(pending.sync)
        pending.action()
    }

    private fun destroy(import: Import) {
        eglReleaseTexImage(display, import.pbuffer, EGL_BACK_BUFFER)
        import.colorBuffer.destroy()
        eglDestroySurface(display, import.pbuffer)
    }

    override fun close() {
        glFinish()
        while (pendingActions.isNotEmpty()) {
            runFirstPendingAction()
        }
        for (import in imports.values) {
            destroy(import)
        }
        imports.clear()
    }

    companion object {
        /**
         * Creates an importer for the current EGL context
         * @return the importer, or null when the context is not an ANGLE context that imports IOSurfaces without
         * copying them
         */
        fun createOrNull(maxImports: Int = 32): IOSurfaceImporterGL3? {
            if (Platform.type != PlatformType.MAC || Driver.glType != DriverTypeGL.GLES ||
                DriverGL3Configuration.glesBackend != GlesBackend.ANGLE
            ) {
                return null
            }
            if (!ensureEGL()) {
                return null
            }
            val display = eglGetCurrentDisplay()
            if (display == EGL_NO_DISPLAY) {
                logger.debug { "no current EGL display" }
                return null
            }
            val displayExtensions = eglQueryString(display, EGL_EXTENSIONS) ?: ""
            val clientExtensions = eglQueryString(EGL_NO_DISPLAY, EGL_EXTENSIONS) ?: ""
            if ("EGL_ANGLE_iosurface_client_buffer" !in displayExtensions ||
                "EGL_ANGLE_feature_control" !in clientExtensions
            ) {
                logger.debug { "ANGLE does not support importing IOSurfaces" }
                return null
            }
            if (!isFeatureEnabled(display, ZERO_COPY_FEATURE)) {
                logger.info { "ANGLE copies IOSurfaces, $ZERO_COPY_FEATURE is not enabled" }
                return null
            }
            val config = chooseConfig(display) ?: return null
            logger.debug { "importing IOSurfaces without copying" }
            return IOSurfaceImporterGL3(display, config, maxImports)
        }

        /**
         * Loads ANGLE's libEGL, which the application extracted to the working directory
         */
        private fun ensureEGL(): Boolean {
            val created = try {
                EGL.getFunctionProvider()
                true
            } catch (e: IllegalStateException) {
                false
            }
            if (!created) {
                val library = File("libEGL.dylib")
                if (!library.exists()) {
                    logger.debug { "ANGLE's libEGL.dylib not found in the working directory" }
                    return false
                }
                EGL.create(library.absolutePath)
            }
            return true
        }

        private fun isFeatureEnabled(display: Long, name: String): Boolean {
            val functionProvider = EGL.getFunctionProvider()
            val queryStringi = functionProvider.getFunctionAddress("eglQueryStringiANGLE")
            val queryDisplayAttrib = functionProvider.getFunctionAddress("eglQueryDisplayAttribANGLE")
            if (queryStringi == 0L || queryDisplayAttrib == 0L) {
                return false
            }
            val count = MemoryStack.stackPush().use { stack ->
                val value = stack.mallocPointer(1)
                if (JNI.invokePPI(display, EGL_FEATURE_COUNT_ANGLE, value.address(), queryDisplayAttrib) == 0) {
                    return false
                }
                value[0].toInt()
            }
            for (i in 0 until count) {
                if (MemoryUtil.memASCIISafe(JNI.invokePP(display, EGL_FEATURE_NAME_ANGLE, i, queryStringi)) == name) {
                    return MemoryUtil.memASCIISafe(JNI.invokePP(display, EGL_FEATURE_STATUS_ANGLE, i, queryStringi)) == "enabled"
                }
            }
            return false
        }

        private fun chooseConfig(display: Long): Long? {
            MemoryStack.stackPush().use { stack ->
                val attributes = stack.ints(
                    EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
                    EGL_BIND_TO_TEXTURE_RGBA, EGL_TRUE,
                    EGL_RED_SIZE, 8,
                    EGL_GREEN_SIZE, 8,
                    EGL_BLUE_SIZE, 8,
                    EGL_ALPHA_SIZE, 8,
                    EGL_NONE
                )
                val configs = stack.mallocPointer(1)
                val configCount = stack.mallocInt(1)
                if (!eglChooseConfig(display, attributes, configs, configCount) || configCount[0] == 0) {
                    logger.error { "no EGL config to bind IOSurfaces to textures" }
                    return null
                }
                val target = stack.mallocInt(1)
                eglGetConfigAttrib(display, configs[0], EGL_BIND_TO_TEXTURE_TARGET_ANGLE, target)
                if (target[0] != EGL_TEXTURE_2D) {
                    logger.debug { "IOSurfaces bind to texture target 0x${target[0].toString(16)}, not EGL_TEXTURE_2D" }
                    return null
                }
                return configs[0]
            }
        }
    }
}

internal actual fun createIOSurfaceImporterGL3(): IOSurfaceImporter? = IOSurfaceImporterGL3.createOrNull()
