@file:OptIn(ExperimentalWasmJsInterop::class)

import org.openrndr.Configuration
import org.openrndr.PinchEvent
import org.openrndr.PointerEvent
import org.openrndr.math.Vector2
import org.openrndr.webgl.updateTouchAction
import web.dom.ElementId
import web.dom.document
import web.html.HTMLCanvasElement
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** dispatches a pointer event at ([x], [y]) in CSS pixels of [canvas] */
private fun dispatchPointer(canvas: HTMLCanvasElement, type: String, id: Int, pointerType: String, x: Double, y: Double): Unit = js(
    """(() => {
        const r = canvas.getBoundingClientRect();
        canvas.dispatchEvent(new PointerEvent(type, {
            pointerId: id, pointerType: pointerType, clientX: r.left + x, clientY: r.top + y,
            pressure: type === 'pointerup' ? 0 : 0.5, isPrimary: id === 1, bubbles: true, cancelable: true
        }));
    })()"""
)

private fun dispatchWheel(canvas: HTMLCanvasElement, deltaY: Double, ctrlKey: Boolean): Unit = js(
    "canvas.dispatchEvent(new WheelEvent('wheel', { deltaY: deltaY, ctrlKey: ctrlKey, bubbles: true, cancelable: true }))"
)

private fun touchAction(canvas: HTMLCanvasElement): String = js("canvas.style.touchAction")

private fun resetTouchAction(canvas: HTMLCanvasElement): Unit = js("canvas.style.touchAction = ''")

/**
 * Touches, pens and pinches trigger the pointer and gesture events of the program, like the SDL back-end
 */
class TestTouchInputWebGL : AbstractApplicationTestFixture() {
    private val canvas get() = document.getElementById(ElementId(Configuration().canvasId)) as HTMLCanvasElement

    private fun close(expected: Vector2, actual: Vector2) =
        assertTrue((expected - actual).length < 1.0, "expected $expected, was $actual")

    @Test
    fun touchesArePointers() {
        val down = mutableListOf<PointerEvent>()
        val moved = mutableListOf<PointerEvent>()
        val up = mutableListOf<PointerEvent>()
        program.pointers.pointerDown.listen { down += it }
        program.pointers.moved.listen { moved += it }
        program.pointers.pointerUp.listen { up += it }

        dispatchPointer(canvas, "pointerdown", 7, "touch", 10.0, 20.0)
        dispatchPointer(canvas, "pointermove", 7, "touch", 15.0, 25.0)
        dispatchPointer(canvas, "pointerup", 7, "touch", 15.0, 25.0)

        assertEquals(listOf(7L), down.map { it.pointerID })
        close(Vector2(10.0, 20.0), down.single().position)
        assertEquals(0.5, down.single().pressure)
        close(Vector2(15.0, 25.0), moved.single().position)
        close(Vector2(5.0, 5.0), moved.single().displacement)
        assertEquals(listOf(7L), up.map { it.pointerID })
    }

    @Test
    fun cancelledTouchesAreUp() {
        val up = mutableListOf<PointerEvent>()
        program.pointers.pointerUp.listen { up += it }
        dispatchPointer(canvas, "pointerdown", 3, "touch", 10.0, 10.0)
        dispatchPointer(canvas, "pointercancel", 3, "touch", 10.0, 10.0)
        assertEquals(listOf(3L), up.map { it.pointerID })
    }

    @Test
    fun miceAreNotPointers() {
        val events = mutableListOf<PointerEvent>()
        program.pointers.pointerDown.listen { events += it }
        program.pointers.moved.listen { events += it }
        dispatchPointer(canvas, "pointerdown", 1, "mouse", 10.0, 10.0)
        dispatchPointer(canvas, "pointermove", 1, "mouse", 20.0, 10.0)
        assertTrue(events.isEmpty())
    }

    @Test
    fun twoTouchesPinch() {
        val pinches = mutableListOf<Pair<String, Double>>()
        program.gestures.pinchStarted.listen { pinches += "started" to it.scale }
        program.gestures.pinchUpdated.listen { pinches += "updated" to it.scale }
        program.gestures.pinchEnded.listen { pinches += "ended" to it.scale }

        dispatchPointer(canvas, "pointerdown", 1, "touch", 10.0, 10.0)
        dispatchPointer(canvas, "pointerdown", 2, "touch", 50.0, 10.0)
        // the distance doubles, then halves
        dispatchPointer(canvas, "pointermove", 2, "touch", 90.0, 10.0)
        dispatchPointer(canvas, "pointermove", 2, "touch", 50.0, 10.0)
        dispatchPointer(canvas, "pointerup", 2, "touch", 50.0, 10.0)
        dispatchPointer(canvas, "pointerup", 1, "touch", 10.0, 10.0)

        assertEquals(listOf("started", "updated", "updated", "ended"), pinches.map { it.first })
        assertEquals(2.0, pinches[1].second, 1E-6)
        assertEquals(0.5, pinches[2].second, 1E-6)
    }

    @Test
    fun controlWheelIsATrackpadPinch() {
        val pinches = mutableListOf<PinchEvent>()
        var scrolled = 0
        program.gestures.pinchStarted.listen { pinches += it }
        program.gestures.pinchUpdated.listen { pinches += it }
        program.mouse.scrolled.listen { scrolled++ }

        dispatchWheel(canvas, -10.0, ctrlKey = true)
        assertEquals(2, pinches.size)
        assertEquals(exp(0.1), pinches[1].scale, 1E-6)
        assertEquals(0, scrolled)

        dispatchWheel(canvas, 10.0, ctrlKey = false)
        assertEquals(1, scrolled)
    }

    @Test
    fun touchesAreNotScrollingWhenListenedTo() {
        // the canvas is shared by the tests
        resetTouchAction(canvas)
        updateTouchAction(canvas, program)
        assertEquals("", touchAction(canvas), "a program that does not listen to touches lets the page scroll")
        program.gestures.pinchUpdated.listen { }
        updateTouchAction(canvas, program)
        assertEquals("none", touchAction(canvas))
    }
}
