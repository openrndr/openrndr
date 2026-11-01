@file:OptIn(ExperimentalWasmJsInterop::class)

package org.openrndr.webgl

import org.openrndr.PinchEvent
import org.openrndr.PointerEvent
import org.openrndr.Program
import org.openrndr.math.Vector2
import web.html.HTMLCanvasElement

private const val POINTER_DOWN = 0
private const val POINTER_MOVED = 1
private const val POINTER_UP = 2

private const val PINCH_STARTED = 0
private const val PINCH_UPDATED = 1
private const val PINCH_ENDED = 2

/**
 * Listens to the touch and pen pointers and pinch gestures of [canvas]:
 * - [onPointer] is called with the event (down, moved, up), the pointer id, its position and displacement in CSS
 *   pixels and its pressure. A cancelled pointer is up, mouse pointers are not reported.
 * - [onPinch] is called with the phase (started, updated, ended) and the scale since the last update. Pinches are
 *   made with two touches, a trackpad pinch is a wheel event with the control key held (Chrome, Firefox), or a gesture
 *   event (Safari).
 * - [capturesTouch] and [capturesPinch] tell if the default actions of the browser, scrolling and zooming, are
 *   prevented.
 */
private fun listenToTouchInputJs(
    canvas: HTMLCanvasElement,
    onPointer: (event: Int, id: Int, x: Double, y: Double, dx: Double, dy: Double, pressure: Double) -> Unit,
    onPinch: (phase: Int, scale: Double) -> Unit,
    capturesTouch: () -> Boolean,
    capturesPinch: () -> Boolean
): Unit = js(
    """(() => {
        const pointers = new Map();
        let pinchDistance = 0;
        const touches = () => [...pointers.values()].filter((p) => p.type === 'touch');
        const distance = (a, b) => Math.hypot(a.x - b.x, a.y - b.y) || 1;

        canvas.addEventListener('pointerdown', (e) => {
            if (e.pointerType === 'mouse') return;
            pointers.set(e.pointerId, { x: e.offsetX, y: e.offsetY, type: e.pointerType });
            // moves outside of the canvas still belong to the touch, capturing fails for pointers that are not active
            if (capturesTouch()) { try { canvas.setPointerCapture(e.pointerId); } catch (error) { } }
            onPointer(0, e.pointerId, e.offsetX, e.offsetY, 0, 0, e.pressure);
            const t = touches();
            if (e.pointerType === 'touch' && t.length === 2) {
                pinchDistance = distance(t[0], t[1]);
                onPinch(0, 1.0);
            }
        });
        canvas.addEventListener('pointermove', (e) => {
            const pointer = pointers.get(e.pointerId);
            if (!pointer) return;
            const dx = e.offsetX - pointer.x, dy = e.offsetY - pointer.y;
            pointer.x = e.offsetX; pointer.y = e.offsetY;
            onPointer(1, e.pointerId, e.offsetX, e.offsetY, dx, dy, e.pressure);
            const t = touches();
            if (pointer.type === 'touch' && t.length >= 2) {
                const d = distance(t[0], t[1]);
                onPinch(1, d / pinchDistance);
                pinchDistance = d;
            }
        });
        const up = (e) => {
            const pointer = pointers.get(e.pointerId);
            if (!pointer) return;
            const pinching = pointer.type === 'touch' && touches().length >= 2;
            pointers.delete(e.pointerId);
            onPointer(2, e.pointerId, e.offsetX, e.offsetY, 0, 0, 0);
            if (pinching && touches().length < 2) onPinch(2, 1.0);
        };
        canvas.addEventListener('pointerup', up);
        canvas.addEventListener('pointercancel', up);

        // trackpad pinches in Chrome and Firefox are wheel events with the control key held
        let wheelPinch = null;
        canvas.addEventListener('wheel', (e) => {
            if (!e.ctrlKey) return;
            if (capturesPinch()) e.preventDefault();
            if (wheelPinch === null) onPinch(0, 1.0); else clearTimeout(wheelPinch);
            const delta = e.deltaY * (e.deltaMode === 1 ? 16 : 1);
            onPinch(1, Math.exp(-delta * 0.01));
            wheelPinch = setTimeout(() => { wheelPinch = null; onPinch(2, 1.0); }, 150);
        }, { passive: false });

        // trackpad pinches in Safari are gesture events, touch pinches are already reported through pointers
        let gestureScale = 1.0;
        let gesturing = false;
        canvas.addEventListener('gesturestart', (e) => {
            if (capturesPinch()) e.preventDefault();
            if (touches().length >= 2) return;
            gesturing = true; gestureScale = 1.0;
            onPinch(0, 1.0);
        });
        canvas.addEventListener('gesturechange', (e) => {
            if (capturesPinch()) e.preventDefault();
            if (!gesturing) return;
            onPinch(1, e.scale / gestureScale);
            gestureScale = e.scale;
        });
        canvas.addEventListener('gestureend', (e) => {
            if (capturesPinch()) e.preventDefault();
            if (!gesturing) return;
            gesturing = false;
            onPinch(2, 1.0);
        });
    })()"""
)

private fun setTouchAction(canvas: HTMLCanvasElement, value: String): Unit = js("canvas.style.touchAction = value")

/**
 * Triggers the pointer events ([Program.pointers]) and pinch gestures ([Program.gestures]) of [program] for the touch
 * input of [canvas], like the SDL back-end: pointers are touches (and pens), a pinch update holds the scale since the
 * previous update.
 */
internal fun listenToTouchInput(canvas: HTMLCanvasElement, program: Program) {
    val pointers = program.pointers
    val gestures = program.gestures
    listenToTouchInputJs(
        canvas,
        onPointer = { event, id, x, y, dx, dy, pressure ->
            val message = PointerEvent(id.toLong(), Vector2(x, y), Vector2(dx, dy), pressure)
            when (event) {
                POINTER_DOWN -> pointers.pointerDown.trigger(message)
                POINTER_MOVED -> pointers.moved.trigger(message)
                POINTER_UP -> pointers.pointerUp.trigger(message)
            }
        },
        onPinch = { phase, scale ->
            when (phase) {
                PINCH_STARTED -> gestures.pinchStarted.trigger(PinchEvent(scale))
                PINCH_UPDATED -> gestures.pinchUpdated.trigger(PinchEvent(scale))
                PINCH_ENDED -> gestures.pinchEnded.trigger(PinchEvent(scale))
            }
        },
        capturesTouch = { listensToTouch(program) },
        capturesPinch = { listensToPinch(program) }
    )
}

private fun listensToTouch(program: Program) = with(program.pointers) {
    pointerDown.listeners.isNotEmpty() || moved.listeners.isNotEmpty() || pointerUp.listeners.isNotEmpty() ||
            listensToPinch(program)
}

private fun listensToPinch(program: Program) = with(program.gestures) {
    pinchStarted.listeners.isNotEmpty() || pinchUpdated.listeners.isNotEmpty() || pinchEnded.listeners.isNotEmpty()
}

/**
 * Prevents the browser from scrolling and zooming on [canvas] with touches when [program] listens to pointers or
 * pinches. The browser decides this when a touch starts, it is set after the setup of the program.
 */
internal fun updateTouchAction(canvas: HTMLCanvasElement, program: Program) {
    if (listensToTouch(program)) {
        setTouchAction(canvas, "none")
    }
}
