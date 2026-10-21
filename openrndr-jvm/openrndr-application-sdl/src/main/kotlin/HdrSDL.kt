package org.openrndr.application.sdl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengles.GLES30
import org.lwjgl.sdl.SDLHints.*
import org.lwjgl.sdl.SDLProperties.SDL_GetPointerProperty
import org.lwjgl.sdl.SDLStdinc.nSDL_malloc
import org.lwjgl.sdl.SDLVideo.*
import org.lwjgl.sdl.SDL_EGLIntArrayCallback
import org.lwjgl.system.JNI.invokePPP
import org.lwjgl.system.JNI.invokePPV
import org.lwjgl.system.MemoryUtil.memPutInt
import org.lwjgl.system.macosx.ObjCRuntime.getLibrary
import org.lwjgl.system.macosx.ObjCRuntime.sel_getUid
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.internal.gl3.DriverTypeGL

private val logger = KotlinLogging.logger { }

private const val GL_FLOAT = 0x1406

private const val EGL_NONE = 0x3038
private const val EGL_GL_COLORSPACE_KHR = 0x309D
private const val EGL_GL_COLORSPACE_SCRGB_LINEAR_EXT = 0x3350

/**
 * Makes SDL create EGL window surfaces in the linear scRGB colorspace, used for HDR windows on
 * OpenGL ES (ANGLE). SDL appends callback attributes after its own colorspace attribute, so SDL's
 * sRGB colorspace attribute is skipped through [SDL_HINT_OPENGL_FORCE_SRGB_FRAMEBUFFER].
 * Must be followed by [uninstallScrgbLinearEglSurface] once the window and its context are created.
 */
internal fun installScrgbLinearEglSurface(): SDL_EGLIntArrayCallback {
    SDL_SetHint(SDL_HINT_OPENGL_FORCE_SRGB_FRAMEBUFFER, "skip")
    val callback = SDL_EGLIntArrayCallback.create { _, _, _ ->
        logger.debug { "Requesting EGL window surface in the scRGB linear colorspace" }
        // SDL frees the returned array with SDL_free
        val attribs = nSDL_malloc(3L * Int.SIZE_BYTES)
        memPutInt(attribs, EGL_GL_COLORSPACE_KHR)
        memPutInt(attribs + 4, EGL_GL_COLORSPACE_SCRGB_LINEAR_EXT)
        memPutInt(attribs + 8, EGL_NONE)
        attribs
    }
    SDL_EGL_SetAttributeCallbacks(null, callback, null, 0L)
    return callback
}

internal fun uninstallScrgbLinearEglSurface(callback: SDL_EGLIntArrayCallback) {
    SDL_EGL_SetAttributeCallbacks(null, null, null, 0L)
    callback.free()
    SDL_ResetHint(SDL_HINT_OPENGL_FORCE_SRGB_FRAMEBUFFER)
}

/**
 * Enables extended dynamic range on the content view of [window], without it macOS clamps the
 * (floating point) OpenGL surface to SDR white. SDL does not do this for OpenGL windows.
 *
 * @return true when EDR was enabled
 */
internal fun enableExtendedDynamicRangeMac(window: Long): Boolean {
    val nsWindow = SDL_GetPointerProperty(SDL_GetWindowProperties(window), SDL_PROP_WINDOW_COCOA_WINDOW_POINTER, 0L)
    if (nsWindow == 0L) {
        logger.warn { "Failed to get the NSWindow of window $window" }
        return false
    }
    val objcMsgSend = getLibrary().getFunctionAddress("objc_msgSend")
    val contentView = invokePPP(nsWindow, sel_getUid("contentView"), objcMsgSend)
    if (contentView == 0L) {
        logger.warn { "Failed to get the content view of window $window" }
        return false
    }
    invokePPV(contentView, sel_getUid("setWantsExtendedDynamicRangeOpenGLSurface:"), true, objcMsgSend)
    return true
}

/**
 * Does the default framebuffer of the current context have floating point color components?
 */
internal fun isDefaultFramebufferFloat(): Boolean {
    val componentType = when (DriverGL3Configuration.driverType) {
        DriverTypeGL.GL -> GL30C.glGetFramebufferAttachmentParameteri(
            GL30C.GL_DRAW_FRAMEBUFFER, GL30C.GL_BACK_LEFT, GL30C.GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE
        )

        DriverTypeGL.GLES -> GLES30.glGetFramebufferAttachmentParameteri(
            GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_BACK, GLES30.GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE
        )
    }
    return componentType == GL_FLOAT
}
