package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.egl.EGL
import org.lwjgl.egl.EGL10.EGL_EXTENSIONS
import org.lwjgl.egl.EGL10.EGL_HEIGHT
import org.lwjgl.egl.EGL10.EGL_NONE
import org.lwjgl.egl.EGL10.EGL_NO_CONTEXT
import org.lwjgl.egl.EGL10.EGL_NO_DISPLAY
import org.lwjgl.egl.EGL10.EGL_WIDTH
import org.lwjgl.egl.EGL14.eglGetCurrentContext
import org.lwjgl.egl.EGL10.eglGetCurrentDisplay
import org.lwjgl.egl.EGL10.eglGetError
import org.lwjgl.egl.EGL10.eglGetProcAddress
import org.lwjgl.egl.EGL10.eglQueryString
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL32C
import org.lwjgl.opengles.GLES30
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.DmaBufImporter
import org.openrndr.internal.Driver
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType

private val logger = KotlinLogging.logger { }

// EGL_EXT_image_dma_buf_import
private const val EGL_LINUX_DMA_BUF_EXT = 0x3270
private const val EGL_LINUX_DRM_FOURCC_EXT = 0x3271
private const val EGL_DMA_BUF_PLANE0_FD_EXT = 0x3272
private const val EGL_DMA_BUF_PLANE0_OFFSET_EXT = 0x3273
private const val EGL_DMA_BUF_PLANE0_PITCH_EXT = 0x3274

// EGL_EXT_image_dma_buf_import_modifiers
private const val EGL_DMA_BUF_PLANE0_MODIFIER_LO_EXT = 0x3443
private const val EGL_DMA_BUF_PLANE0_MODIFIER_HI_EXT = 0x3444

private const val EGL_NO_IMAGE = 0L

/** builds a DRM fourcc code, see drm_fourcc.h */
private fun fourcc(code: String): Int =
    code[0].code or (code[1].code shl 8) or (code[2].code shl 16) or (code[3].code shl 24)

private val DRM_FORMAT_R8 = fourcc("R8  ")
private val DRM_FORMAT_GR88 = fourcc("GR88")
private val DRM_FORMAT_RG88 = fourcc("RG88")
private val DRM_FORMAT_R16 = fourcc("R16 ")
private val DRM_FORMAT_GR1616 = fourcc("GR32")
private val DRM_FORMAT_RG1616 = fourcc("RG32")

private fun planeFormat(fourcc: Int): Pair<ColorFormat, ColorType>? = when (fourcc) {
    DRM_FORMAT_R8 -> ColorFormat.R to ColorType.UINT8
    // nvidia-vaapi-driver exports the chroma planes of NV12 and P010 frames as RG88 and RG1616
    DRM_FORMAT_GR88, DRM_FORMAT_RG88 -> ColorFormat.RG to ColorType.UINT8
    DRM_FORMAT_R16 -> ColorFormat.R to ColorType.UINT16
    DRM_FORMAT_GR1616, DRM_FORMAT_RG1616 -> ColorFormat.RG to ColorType.UINT16
    else -> null
}

/**
 * Imports DMA-BUF planes as EGL images through EGL_EXT_image_dma_buf_import, and binds them to textures with
 * GL_OES_EGL_image. Works in EGL contexts only.
 */
internal class DmaBufImporterGL3 private constructor(
    private val display: Long,
    private val supportsModifiers: Boolean,
    private val createImage: Long,
    private val destroyImage: Long,
    private val imageTargetTexture2D: Long,
) : DmaBufImporter {

    /** EGL images by imported color buffer */
    private val imports = mutableMapOf<ColorBuffer, Long>()

    private class PendingAction(val sync: Long, val action: () -> Unit)

    private val pendingActions = ArrayDeque<PendingAction>()

    private var unsupportedFormatLogged = false

    override fun import(
        fd: Int, width: Int, height: Int, fourcc: Int, offset: Int, pitch: Int, modifier: Long
    ): ColorBuffer? {
        val (format, type) = planeFormat(fourcc) ?: run {
            if (!unsupportedFormatLogged) {
                logger.warn { "can not import DMA-BUF with DRM format 0x${fourcc.toString(16)}" }
                unsupportedFormatLogged = true
            }
            return null
        }

        val image = MemoryStack.stackPush().use { stack ->
            val attributes = mutableListOf(
                EGL_WIDTH, width,
                EGL_HEIGHT, height,
                EGL_LINUX_DRM_FOURCC_EXT, fourcc,
                EGL_DMA_BUF_PLANE0_FD_EXT, fd,
                EGL_DMA_BUF_PLANE0_OFFSET_EXT, offset,
                EGL_DMA_BUF_PLANE0_PITCH_EXT, pitch,
            )
            if (supportsModifiers && modifier != DmaBufImporter.DRM_FORMAT_MOD_INVALID) {
                attributes += listOf(
                    EGL_DMA_BUF_PLANE0_MODIFIER_LO_EXT, (modifier and 0xffffffffL).toInt(),
                    EGL_DMA_BUF_PLANE0_MODIFIER_HI_EXT, (modifier ushr 32).toInt(),
                )
            }
            attributes += EGL_NONE
            val attributeList = stack.ints(*attributes.toIntArray())
            // eglCreateImageKHR(display, context, target, buffer, attributes)
            JNI.invokePPPPP(
                display, EGL_NO_CONTEXT, EGL_LINUX_DMA_BUF_EXT, 0L, MemoryUtil.memAddress(attributeList), createImage
            )
        }
        if (image == EGL_NO_IMAGE) {
            logger.error { "failed to create EGL image for DMA-BUF: EGL error 0x${eglGetError().toString(16)}" }
            return null
        }

        val previousTexture = IntArray(1)
        glGetIntegerv(GL_TEXTURE_BINDING_2D, previousTexture)
        val texture = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, texture)
        JNI.invokePV(GL_TEXTURE_2D, image, imageTargetTexture2D)
        // the texture has no mipmaps, the default minification filter would make it incomplete
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glBindTexture(GL_TEXTURE_2D, previousTexture[0])

        val error = glGetError()
        if (error != GL_NO_ERROR) {
            logger.error { "failed to bind EGL image to texture: GL error 0x${error.toString(16)}" }
            glDeleteTextures(texture)
            JNI.invokePPI(display, image, destroyImage)
            return null
        }

        val colorBuffer = ColorBufferGL3(
            DriverGL3.generateResourceId(), GL_TEXTURE_2D, texture, width, height, 1.0,
            format, type, 1, BufferMultisample.Disabled, null
        )
        imports[colorBuffer] = image
        return colorBuffer
    }

    override fun release(colorBuffer: ColorBuffer) {
        val image = imports.remove(colorBuffer) ?: return
        colorBuffer.destroy()
        JNI.invokePPI(display, image, destroyImage)
    }

    override fun runWhenGpuFinished(action: () -> Unit) {
        val sync = when (Driver.glType) {
            DriverTypeGL.GL -> GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
            DriverTypeGL.GLES -> GLES30.glFenceSync(GLES30.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
        }
        pendingActions.addLast(PendingAction(sync, action))
    }

    private fun clientWaitSync(sync: Long, timeout: Long): Int = when (Driver.glType) {
        DriverTypeGL.GL -> GL32C.glClientWaitSync(sync, GL32C.GL_SYNC_FLUSH_COMMANDS_BIT, timeout)
        DriverTypeGL.GLES -> GLES30.glClientWaitSync(sync, GLES30.GL_SYNC_FLUSH_COMMANDS_BIT, timeout)
    }

    override fun update() {
        // fences are signaled in order
        while (pendingActions.isNotEmpty()) {
            val status = clientWaitSync(pendingActions.first().sync, 0L)
            if (status == GL32C.GL_TIMEOUT_EXPIRED) {
                break
            }
            if (status == GL32C.GL_WAIT_FAILED) {
                logger.error { "waiting for fence failed" }
            }
            runFirstPendingAction()
        }
    }

    private fun runFirstPendingAction() {
        val pending = pendingActions.removeFirst()
        when (Driver.glType) {
            DriverTypeGL.GL -> GL32C.glDeleteSync(pending.sync)
            DriverTypeGL.GLES -> GLES30.glDeleteSync(pending.sync)
        }
        pending.action()
    }

    override fun close() {
        glFinish()
        while (pendingActions.isNotEmpty()) {
            runFirstPendingAction()
        }
        for (colorBuffer in imports.keys.toList()) {
            release(colorBuffer)
        }
    }

    companion object {
        /**
         * Creates an importer for the current context
         * @return the importer, or null when the context is not an EGL context that imports DMA-BUFs
         */
        fun createOrNull(): DmaBufImporterGL3? {
            if (Platform.type != PlatformType.GENERIC) {
                return null
            }
            if (!ensureEGL()) {
                return null
            }
            val display = eglGetCurrentDisplay()
            if (display == EGL_NO_DISPLAY || eglGetCurrentContext() == EGL_NO_CONTEXT) {
                logger.info { "the context is not an EGL context, DMA-BUFs can not be imported" }
                return null
            }
            val extensions = (eglQueryString(display, EGL_EXTENSIONS) ?: "").split(' ').toSet()
            if ("EGL_EXT_image_dma_buf_import" !in extensions || "EGL_KHR_image_base" !in extensions) {
                logger.info { "EGL does not support importing DMA-BUFs" }
                return null
            }
            if (!hasGLExtension("GL_OES_EGL_image")) {
                logger.info { "GL_OES_EGL_image is not supported" }
                return null
            }
            val createImage = eglGetProcAddress("eglCreateImageKHR")
            val destroyImage = eglGetProcAddress("eglDestroyImageKHR")
            val imageTargetTexture2D = eglGetProcAddress("glEGLImageTargetTexture2DOES")
            if (createImage == 0L || destroyImage == 0L || imageTargetTexture2D == 0L) {
                logger.info { "EGL image functions not found" }
                return null
            }
            logger.debug { "importing DMA-BUFs without copying" }
            return DmaBufImporterGL3(
                display,
                "EGL_EXT_image_dma_buf_import_modifiers" in extensions,
                createImage,
                destroyImage,
                imageTargetTexture2D
            )
        }

        private fun hasGLExtension(name: String): Boolean {
            val count = IntArray(1)
            glGetIntegerv(GL_NUM_EXTENSIONS, count)
            return (0 until count[0]).any {
                when (Driver.glType) {
                    DriverTypeGL.GL -> GL30C.glGetStringi(GL_EXTENSIONS, it)
                    DriverTypeGL.GLES -> GLES30.glGetStringi(GL_EXTENSIONS, it)
                } == name
            }
        }

        /**
         * Loads libEGL when it was not loaded by the application back-end, the context is not an EGL context when
         * libEGL is not available
         */
        private fun ensureEGL(): Boolean {
            return try {
                EGL.getFunctionProvider()
                true
            } catch (e: IllegalStateException) {
                try {
                    EGL.create()
                    true
                } catch (e: Throwable) {
                    logger.debug(e) { "libEGL could not be loaded" }
                    false
                }
            }
        }
    }
}

internal actual fun createDmaBufImporterGL3(): DmaBufImporter? = DmaBufImporterGL3.createOrNull()
