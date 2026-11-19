package org.openrndr.awt

import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.opengl.awt.AWTGLCanvas
import org.lwjgl.opengl.awt.GLData
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLESCapabilities
import org.openrndr.WindowMultisample
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import java.awt.Canvas

/**
 * The context attributes of OPENRNDR's OpenGL or OpenGL ES contexts
 */
internal fun glData(vsync: Boolean, multisample: WindowMultisample): GLData = GLData().apply {
    when (DriverGL3Configuration.driverType) {
        DriverTypeGL.GL -> {
            api = GLData.API.GL
            majorVersion = 3
            minorVersion = 3
            profile = GLData.Profile.CORE
            forwardCompatible = true
        }

        DriverTypeGL.GLES -> {
            api = GLData.API.GLES
            majorVersion = 3
            minorVersion = 0
        }
    }
    versionPolicy = GLData.VersionPolicy.AT_LEAST
    redSize = 8
    greenSize = 8
    blueSize = 8
    alphaSize = 8
    depthSize = 24
    stencilSize = 8
    doubleBuffer = true
    // OPENRNDR draws linear colors in sRGB framebuffers, macOS does not take the attribute, its framebuffers are sRGB
    sRGB = Platform.type != PlatformType.MAC
    swapInterval = if (vsync) 1 else 0
    if (multisample is WindowMultisample.SampleCount) {
        samples = multisample.count
    }
}

/**
 * A canvas that is drawn with OpenGL, or OpenGL ES on Windows and Linux, through lwjgl3-awt
 */
internal class GLCanvas(data: GLData) : AWTGLCanvas(data), CanvasSurface {
    override val canvas: Canvas get() = this
    override val pixelWidth: Int get() = getFramebufferWidth()
    override val pixelHeight: Int get() = getFramebufferHeight()

    private var frame: (() -> Unit)? = null
    private var glCapabilities: GLCapabilities? = null
    private var glesCapabilities: GLESCapabilities? = null

    init {
        ignoreRepaint = true
    }

    override fun initGL() {
        when (DriverGL3Configuration.driverType) {
            DriverTypeGL.GL -> glCapabilities = GL.createCapabilities()
            DriverTypeGL.GLES -> glesCapabilities = GLES.createCapabilities()
        }
    }

    override fun paintGL() {
        glCapabilities?.let { GL.setCapabilities(it) }
        glesCapabilities?.let { GLES.setCapabilities(it) }
        AWTRenderLoop.contextId = context
        frame?.invoke()
        swapBuffers()
    }

    override fun render(frame: () -> Unit): Boolean {
        if (!isDisplayable || !isShowing || width <= 0 || height <= 0) {
            return false
        }
        this.frame = frame
        try {
            render()
        } finally {
            this.frame = null
        }
        return true
    }
}
