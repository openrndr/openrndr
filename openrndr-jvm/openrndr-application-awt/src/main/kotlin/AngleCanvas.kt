package org.openrndr.awt

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.awt.AWT
import org.lwjgl.awt.MacOSX
import org.lwjgl.egl.EGL15.*
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLESCapabilities
import org.lwjgl.system.MemoryStack.stackPush
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.internal.gl3.DriverTypeGL
import java.awt.Canvas
import java.awt.event.HierarchyEvent
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger { }

/**
 * The EGL display, config and context of ANGLE. All ANGLE canvases share one context, they draw in turn on the render
 * thread.
 */
internal object AngleEGL {
    val display: Long
    val config: Long
    val context: Long
    var capabilities: GLESCapabilities? = null

    init {
        display = eglGetDisplay(EGL_DEFAULT_DISPLAY)
        require(display != EGL_NO_DISPLAY) { "no EGL display" }
        stackPush().use { stack ->
            val major = stack.mallocInt(1)
            val minor = stack.mallocInt(1)
            require(eglInitialize(display, major, minor)) { "eglInitialize failed: ${eglGetError()}" }
            logger.debug { "EGL ${major[0]}.${minor[0]}, ${eglQueryString(display, EGL_VENDOR)}" }
            require(eglBindAPI(EGL_OPENGL_ES_API)) { "eglBindAPI failed: ${eglGetError()}" }

            val attributes = stack.ints(
                EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
                EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
                EGL_RED_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_BLUE_SIZE, 8,
                EGL_ALPHA_SIZE, 8,
                EGL_DEPTH_SIZE, 24,
                EGL_STENCIL_SIZE, 8,
                EGL_NONE
            )
            val configs = stack.mallocPointer(1)
            val count = stack.mallocInt(1)
            require(eglChooseConfig(display, attributes, configs, count) && count[0] > 0) {
                "no EGL config: ${eglGetError()}"
            }
            config = configs[0]

            var created = EGL_NO_CONTEXT
            for (version in DriverGL3Configuration.candidateVersions().filter { it.type == DriverTypeGL.GLES }) {
                val contextAttributes = stack.ints(
                    EGL_CONTEXT_MAJOR_VERSION, version.majorVersion,
                    EGL_CONTEXT_MINOR_VERSION, version.minorVersion,
                    EGL_NONE
                )
                created = eglCreateContext(display, config, EGL_NO_CONTEXT, contextAttributes)
                if (created != EGL_NO_CONTEXT) {
                    logger.debug { "created OpenGL ES ${version.majorVersion}.${version.minorVersion} context" }
                    break
                }
            }
            require(created != EGL_NO_CONTEXT) { "failed to create an OpenGL ES context: ${eglGetError()}" }
            context = created
        }
    }
}

/**
 * A canvas that is drawn with OpenGL ES through ANGLE, on macOS.
 *
 * The canvas gets a Core Animation layer through JAWT, ANGLE draws in a Metal layer that it adds to it. lwjgl3-awt
 * creates OpenGL contexts with CGL on macOS, it does not create OpenGL ES contexts.
 */
internal class AngleCanvas(private val vsync: Boolean) : Canvas(), CanvasSurface {
    override val canvas: Canvas get() = this

    /** guards the surface against [removeNotify] while it is drawn */
    private val lock = Any()

    @Volatile
    private var scale = 1.0

    /** the JAWT surface layers of the canvas, retained */
    private var surfaceLayers = 0L

    /** the layer of the canvas, retained, ANGLE adds its Metal layer to it */
    private var layer = 0L
    private var surface = EGL_NO_SURFACE
    private var layerFrame = IntArray(4)
    private var layerScale = 0.0

    override val pixelWidth: Int get() = (width * scale).roundToInt()
    override val pixelHeight: Int get() = (height * scale).roundToInt()

    init {
        ignoreRepaint = true
        addHierarchyListener { e ->
            if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                synchronized(lock) {
                    if (layer != 0L) {
                        MacOSLayers.setHidden(layer, !isShowing)
                    }
                }
            }
        }
        addPropertyChangeListener("graphicsConfiguration") { updateScale() }
    }

    private fun updateScale() {
        scale = graphicsConfiguration?.defaultTransform?.scaleX ?: 1.0
    }

    override fun addNotify() {
        super.addNotify()
        updateScale()
    }

    override fun removeNotify() {
        synchronized(lock) {
            destroySurface()
        }
        super.removeNotify()
    }

    override fun render(frame: () -> Unit): Boolean = synchronized(lock) {
        if (!isDisplayable || !isShowing || width <= 0 || height <= 0) {
            return false
        }
        if (surface == EGL_NO_SURFACE && !createSurface()) {
            return false
        }
        updateLayer()

        val egl = AngleEGL
        check(eglMakeCurrent(egl.display, surface, surface, egl.context)) { "eglMakeCurrent failed: ${eglGetError()}" }
        val capabilities = egl.capabilities
        if (capabilities == null) {
            egl.capabilities = GLES.createCapabilities()
        } else {
            GLES.setCapabilities(capabilities)
        }
        eglSwapInterval(egl.display, if (vsync) 1 else 0)
        AWTRenderLoop.contextId = egl.context
        try {
            frame()
            eglSwapBuffers(egl.display, surface)
        } finally {
            // the surface is released, so the event thread can destroy it while the context stays current
            eglMakeCurrent(egl.display, EGL_NO_SURFACE, EGL_NO_SURFACE, egl.context)
        }
        true
    }

    private fun createSurface(): Boolean {
        val platformInfo = try {
            AWT(this).use { MacOSLayers.retain(it.platformInfo) }
        } catch (e: java.awt.AWTException) {
            logger.debug(e) { "the canvas has no drawing surface yet" }
            return false
        }
        surfaceLayers = platformInfo
        val frame = layerBounds(this)
        layer = MacOSLayers.createLayer(
            frame[0].toDouble(), frame[1].toDouble(), frame[2].toDouble(), frame[3].toDouble(), scale
        )
        layerFrame = frame
        layerScale = scale
        MacOSLayers.setSurfaceLayer(surfaceLayers, layer)

        val egl = AngleEGL
        // OPENRNDR draws linear colors in sRGB framebuffers, like the sRGB capable windows of the other back-ends
        surface = stackPush().use {
            eglCreateWindowSurface(
                egl.display, egl.config, layer, it.ints(EGL_GL_COLORSPACE, EGL_GL_COLORSPACE_SRGB, EGL_NONE)
            )
        }
        // ANGLE adds its Metal layer on this thread, which has no run loop to commit the transaction
        MacOSX.caFlush()
        if (surface == EGL_NO_SURFACE) {
            logger.error { "eglCreateWindowSurface failed: ${eglGetError()}" }
            destroySurface()
            return false
        }
        return true
    }

    /** follows the bounds and the content scale of the canvas */
    private fun updateLayer() {
        val frame = layerBounds(this)
        if (!frame.contentEquals(layerFrame)) {
            layerFrame = frame
            MacOSLayers.setFrame(layer, frame[0].toDouble(), frame[1].toDouble(), frame[2].toDouble(), frame[3].toDouble())
        }
        if (scale != layerScale) {
            layerScale = scale
            MacOSLayers.setContentsScale(layer, scale)
        }
    }

    private fun destroySurface() {
        if (surface != EGL_NO_SURFACE) {
            eglDestroySurface(AngleEGL.display, surface)
            surface = EGL_NO_SURFACE
        }
        if (surfaceLayers != 0L) {
            MacOSLayers.setSurfaceLayer(surfaceLayers, 0L)
            MacOSLayers.release(surfaceLayers)
            surfaceLayers = 0L
        }
        if (layer != 0L) {
            MacOSLayers.release(layer)
            layer = 0L
        }
    }
}
