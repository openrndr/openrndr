package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.WGLNVDXInterop.WGL_ACCESS_READ_ONLY_NV
import org.lwjgl.opengl.WGLNVDXInterop.wglDXCloseDeviceNV
import org.lwjgl.opengl.WGLNVDXInterop.wglDXLockObjectsNV
import org.lwjgl.opengl.WGLNVDXInterop.wglDXOpenDeviceNV
import org.lwjgl.opengl.WGLNVDXInterop.wglDXRegisterObjectNV
import org.lwjgl.opengl.WGLNVDXInterop.wglDXUnlockObjectsNV
import org.lwjgl.opengl.WGLNVDXInterop.wglDXUnregisterObjectNV
import org.lwjgl.system.MemoryStack
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.D3D11TextureImporter
import org.openrndr.internal.Driver
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType

private val logger = KotlinLogging.logger { }

/**
 * Imports Direct3D 11 textures through WGL_NV_DX_interop2, which shares the memory of the texture with OpenGL
 */
internal class D3D11TextureImporterGL3 private constructor(
    /** the interop handle of the Direct3D device */
    private val interopDevice: Long
) : D3D11TextureImporter {

    /** interop handles by imported color buffer */
    private val imports = mutableMapOf<ColorBuffer, Long>()

    override fun import(texture: Long, width: Int, height: Int, format: ColorFormat, type: ColorType): ColorBuffer? {
        val glTexture = glGenTextures()
        val interopObject = wglDXRegisterObjectNV(
            interopDevice, texture, glTexture, GL_TEXTURE_2D, WGL_ACCESS_READ_ONLY_NV
        )
        if (interopObject == 0L) {
            logger.error { "failed to register Direct3D texture with OpenGL" }
            glDeleteTextures(glTexture)
            return null
        }

        // the texture has no mipmaps, the default minification filter would make it incomplete
        val previousTexture = IntArray(1)
        glGetIntegerv(GL_TEXTURE_BINDING_2D, previousTexture)
        glBindTexture(GL_TEXTURE_2D, glTexture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glBindTexture(GL_TEXTURE_2D, previousTexture[0])

        val colorBuffer = ColorBufferGL3(
            DriverGL3.generateResourceId(), GL_TEXTURE_2D, glTexture, width, height, 1.0,
            format, type, 1, BufferMultisample.Disabled, null
        )
        imports[colorBuffer] = interopObject
        return colorBuffer
    }

    private fun interopObject(colorBuffer: ColorBuffer) =
        imports[colorBuffer] ?: error("color buffer was not imported by this importer")

    override fun lock(colorBuffer: ColorBuffer): Boolean {
        val interopObject = interopObject(colorBuffer)
        MemoryStack.stackPush().use { stack ->
            if (!wglDXLockObjectsNV(interopDevice, stack.pointers(interopObject))) {
                logger.error { "failed to lock Direct3D texture" }
                return false
            }
        }
        return true
    }

    override fun unlock(colorBuffer: ColorBuffer) {
        val interopObject = interopObject(colorBuffer)
        MemoryStack.stackPush().use { stack ->
            wglDXUnlockObjectsNV(interopDevice, stack.pointers(interopObject))
        }
    }

    override fun release(colorBuffer: ColorBuffer) {
        val interopObject = imports.remove(colorBuffer) ?: return
        wglDXUnregisterObjectNV(interopDevice, interopObject)
        colorBuffer.destroy()
    }

    override fun close() {
        for (colorBuffer in imports.keys.toList()) {
            release(colorBuffer)
        }
        wglDXCloseDeviceNV(interopDevice)
    }

    companion object {
        fun isSupported(): Boolean {
            if (Platform.type != PlatformType.WINDOWS || Driver.glType != DriverTypeGL.GL) {
                return false
            }
            return try {
                GL.getCapabilitiesWGL().WGL_NV_DX_interop2
            } catch (e: Throwable) {
                logger.debug(e) { "failed to query WGL extensions" }
                false
            }
        }

        /**
         * Creates an importer for [device], an `ID3D11Device`, in the current context
         * @return the importer, or null when the context can not share textures with [device]
         */
        fun createOrNull(device: Long): D3D11TextureImporterGL3? {
            if (!isSupported()) {
                return null
            }
            // fails when the device is on another adapter than the context
            val interopDevice = wglDXOpenDeviceNV(device)
            if (interopDevice == 0L) {
                logger.info { "can not share textures of the Direct3D 11 device with OpenGL" }
                return null
            }
            logger.debug { "importing Direct3D 11 textures without copying" }
            return D3D11TextureImporterGL3(interopDevice)
        }
    }
}

internal actual fun supportsD3D11TextureImportGL3(): Boolean = D3D11TextureImporterGL3.isSupported()

internal actual fun createD3D11TextureImporterGL3(device: Long): D3D11TextureImporter? =
    D3D11TextureImporterGL3.createOrNull(device)
