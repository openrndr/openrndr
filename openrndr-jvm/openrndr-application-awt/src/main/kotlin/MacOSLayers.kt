package org.openrndr.awt

import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.system.macosx.ObjCRuntime
import java.nio.ByteBuffer

/**
 * Core Animation layers for AWT components on macOS, through the Objective-C runtime.
 *
 * Layer changes are queued on AppKit's main thread and not waited for: the caller may be AWT's event thread while
 * AppKit waits for AWT, the same approach as lwjgl3-awt's macOS canvas.
 */
internal object MacOSLayers {
    private val objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend")
    private val objc_autoreleasePoolPush = ObjCRuntime.getLibrary().getFunctionAddress("objc_autoreleasePoolPush")
    private val objc_autoreleasePoolPop = ObjCRuntime.getLibrary().getFunctionAddress("objc_autoreleasePoolPop")

    private fun sel(name: String) = ObjCRuntime.sel_getUid(name)

    /** kCALayerWidthSizable | kCALayerHeightSizable */
    private const val AUTORESIZE_WIDTH_HEIGHT = 2L or 16L

    /**
     * Creates a retained, empty CALayer of the given frame and contents scale, release it with [release]
     */
    fun createLayer(x: Double, y: Double, width: Double, height: Double, contentsScale: Double): Long {
        val allocated = JNI.invokePPP(ObjCRuntime.objc_getClass("CALayer"), sel("alloc"), objc_msgSend)
        val layer = JNI.invokePPP(allocated, sel("init"), objc_msgSend)
        require(layer != NULL) { "failed to create a CALayer" }
        JNI.invokePPPV(layer, sel("setAutoresizingMask:"), AUTORESIZE_WIDTH_HEIGHT, objc_msgSend)
        // the layer is not part of a layer tree yet, it is configured on this thread
        invokeNow(layer, "setFrame:") { it.doubleBytes(x, y, width, height) }
        invokeNow(layer, "setContentsScale:") { it.doubleBytes(contentsScale) }
        return layer
    }

    fun retain(target: Long): Long = JNI.invokePPP(target, sel("retain"), objc_msgSend)

    fun release(target: Long) = performOnMainThread(target, "release", NULL)

    /** sets [layer] as the layer of the JAWT surface layers [surfaceLayers], null removes the layer */
    fun setSurfaceLayer(surfaceLayers: Long, layer: Long) = performOnMainThread(surfaceLayers, "setLayer:", layer)

    fun setFrame(layer: Long, x: Double, y: Double, width: Double, height: Double) =
        invokeOnMainThread(layer, "setFrame:") { it.doubleBytes(x, y, width, height) }

    fun setContentsScale(layer: Long, scale: Double) =
        invokeOnMainThread(layer, "setContentsScale:") { it.doubleBytes(scale) }

    fun setHidden(layer: Long, hidden: Boolean) =
        invokeOnMainThread(layer, "setHidden:") { it.bytes(if (hidden) 1 else 0) }

    private fun performOnMainThread(target: Long, selector: String, argument: Long) {
        JNI.callPPPPV(
            target, sel("performSelectorOnMainThread:withObject:waitUntilDone:"), sel(selector), argument,
            ObjCRuntime.NO.toInt(), objc_msgSend
        )
    }

    /**
     * Sends [selector] with one argument to [target] on this thread, the argument is read from the buffer made by
     * [argument]
     */
    private fun invokeNow(target: Long, selector: String, argument: (MemoryStack) -> ByteBuffer) {
        val invocation = invocation(target, selector, argument)
        JNI.invokePPV(invocation, sel("invoke"), objc_msgSend)
    }

    /**
     * Queues [selector] with one argument for [target] on AppKit's main thread, which commits the change to the
     * layer tree. NSInvocation copies the argument, so stack memory can be used for it.
     */
    private fun invokeOnMainThread(target: Long, selector: String, argument: (MemoryStack) -> ByteBuffer) {
        val pool = JNI.invokeP(objc_autoreleasePoolPush)
        try {
            val invocation = invocation(target, selector, argument)
            JNI.invokePPP(invocation, sel("retain"), objc_msgSend)
            performOnMainThread(invocation, "invoke", NULL)
            performOnMainThread(invocation, "release", NULL)
        } finally {
            JNI.invokePV(pool, objc_autoreleasePoolPop)
        }
    }

    /** an autoreleased NSInvocation of [selector] for [target] with its argument set */
    private fun invocation(target: Long, selector: String, argument: (MemoryStack) -> ByteBuffer): Long {
        val selectorId = sel(selector)
        val signature = JNI.invokePPPP(target, sel("methodSignatureForSelector:"), selectorId, objc_msgSend)
        require(signature != NULL) { "no method $selector" }
        val invocation = JNI.invokePPPP(
            ObjCRuntime.objc_getClass("NSInvocation"), sel("invocationWithMethodSignature:"), signature, objc_msgSend
        )
        JNI.invokePPPV(invocation, sel("setTarget:"), target, objc_msgSend)
        JNI.invokePPPV(invocation, sel("setSelector:"), selectorId, objc_msgSend)
        MemoryStack.stackPush().use { stack ->
            val value = argument(stack)
            JNI.invokePPPV(invocation, sel("setArgument:atIndex:"), memAddress(value), 2, objc_msgSend)
        }
        return invocation
    }

    private fun MemoryStack.bytes(value: Int): ByteBuffer = malloc(1).put(0, value.toByte())

    private fun MemoryStack.doubleBytes(vararg values: Double): ByteBuffer {
        val buffer = malloc(8 * values.size)
        values.forEachIndexed { index, value -> buffer.putDouble(index * 8, value) }
        return buffer
    }
}
