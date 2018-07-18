package org.openrndr.extensions

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Matrix44
import org.openrndr.math.Spherical
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.lookAt as lookAt_

class OrbitalCamera(eye: Vector3, lookAt: Vector3) {

    // current position in spherical coordinates
    var spherical = Spherical.fromVector(eye)
        private set
    var lookAt = lookAt
        private set

    private var sphericalEnd = Spherical.fromVector(eye)
    private var lookAtEnd = lookAt.copy()
    private var dirty: Boolean = true

    var dampingFactor = 0.05
    var zoomSpeed = 1.0

    fun rotateTo(rotX: Double, rotY: Double) {
        sphericalEnd += Spherical(0.0, rotX, rotY)
        sphericalEnd = sphericalEnd.makeSafe()

        dirty = true
    }

    fun dollyIn() {
        val zoomScale = Math.pow(0.95, zoomSpeed)
        dollyTo(sphericalEnd.radius * zoomScale - sphericalEnd.radius)
    }

    fun dollyOut() {
        val zoomScale = Math.pow(0.95, zoomSpeed)
        dollyTo(sphericalEnd.radius / zoomScale - sphericalEnd.radius)
    }

    private fun dollyTo(distance: Double) {
        sphericalEnd += Spherical(distance, 0.0, 0.0)
        dirty = true
    }

    fun pan(x: Double, y: Double) {
        val view = viewMatrix()
        val xColumn = Vector3(view.c0r0, view.c0r1, view.c0r2) * x
        val yColumn = Vector3(view.c1r0, view.c1r1, view.c1r2) * -y
        lookAtEnd += xColumn + yColumn
        dirty = true
    }

    fun update(timeDelta: Double) {
        if (!dirty) return
        dirty = false

        val dampingFactor = dampingFactor * timeDelta / 0.0060
        val sphericalDelta = sphericalEnd - spherical

        val lookAtDelta = lookAtEnd - lookAt

        if (
                Math.abs(sphericalEnd.radius) > EPSILON ||
                Math.abs(sphericalEnd.theta) > EPSILON ||
                Math.abs(sphericalEnd.phi) > EPSILON ||
                Math.abs(lookAtDelta.x) > EPSILON ||
                Math.abs(lookAtDelta.y) > EPSILON ||
                Math.abs(lookAtDelta.z) > EPSILON
        ) {

            spherical += (sphericalDelta * dampingFactor)
            lookAt += (lookAtDelta * dampingFactor)
            dirty = true

        } else {
            spherical = sphericalEnd.copy()
            lookAt = lookAtEnd.copy()
        }
        spherical = spherical.makeSafe()
    }

    fun viewMatrix(): Matrix44 {
        return lookAt_(Vector3.fromSpherical(spherical) + lookAt, lookAt, Vector3.UNIT_Y)
    }

    companion object {
        private const val EPSILON = 0.000001
    }
}

class OrbitalControls(val orbitalCamera: OrbitalCamera) {
    enum class STATE {
        NONE,
        ROTATE,
        PAN,
    }

    private var state = STATE.NONE
    var fov = 90.0

    private lateinit var program: Program
    private lateinit var lastMousePosition: Vector2

    private fun mouseScrolled(event: Program.Mouse.MouseEvent) {

        if (Math.abs(event.rotation.x) > 0.1) return

        when {
            event.rotation.y > 0 -> orbitalCamera.dollyIn()
            event.rotation.y < 0 -> orbitalCamera.dollyOut()
        }
    }

    private fun mouseMoved(event: Program.Mouse.MouseEvent) {

        if (state == STATE.NONE) return

        val delta = lastMousePosition - event.position
        lastMousePosition = event.position

        if (KeyboardModifier.SHIFT in event.modifiers) {

            val offset = Vector3.fromSpherical(orbitalCamera.spherical) - orbitalCamera.lookAt

            // half of the fov is center to top of screen
            val targetDistance = offset.length * Math.tan((fov / 2) * Math.PI / 180)
            val panX = (2 * delta.x * targetDistance / program.window.size.x)
            val panY = (2 * delta.y * targetDistance / program.window.size.y)

            orbitalCamera.pan(panX, panY)

        } else {
            val rotX = 2 * Math.PI * delta.x / program.window.size.x
            val rotY = 2 * Math.PI * delta.y / program.window.size.y
            orbitalCamera.rotateTo(rotX, rotY)
        }

    }

    private fun mouseButtonDown(event: Program.Mouse.MouseEvent) {
        val previousState = state

        when (event.button) {
            MouseButton.LEFT -> {
                state = STATE.ROTATE
            }
            MouseButton.RIGHT -> {
                state = STATE.PAN
            }
            MouseButton.CENTER -> {
            }
            MouseButton.NONE -> {
            }
        }

        if (previousState == STATE.NONE) {
            lastMousePosition = event.position
        }
    }

    fun keyPressed(keyEvent: KeyEvent) {
        if (keyEvent.key == KEY_ARROW_RIGHT) {
            orbitalCamera.pan(1.0, 0.0)
        }
        if (keyEvent.key == KEY_ARROW_LEFT) {
            orbitalCamera.pan(-1.0, 0.0)
        }
        if (keyEvent.key == KEY_ARROW_UP) {
            orbitalCamera.pan(0.0, 1.0)
        }
        if (keyEvent.key == KEY_ARROW_DOWN) {
            orbitalCamera.pan(0.0, -1.0)
        }
    }

    fun setup(program: Program) {
        this.program = program
        program.mouse.moved.listen { mouseMoved(it) }
        program.mouse.buttonDown.listen { mouseButtonDown(it) }
        program.mouse.buttonUp.listen { state = STATE.NONE }
        program.mouse.scrolled.listen { mouseScrolled(it) }
        program.keyboard.keyDown.listen { keyPressed(it) }
        program.keyboard.keyRepeat.listen{ keyPressed(it) }
    }
}

@Suppress("unused")
class Debug3D(eye: Vector3 = Vector3(0.0, 0.0, 10.0), lookAt: Vector3 = Vector3.ZERO, private val fov: Double = 90.0) : Extension {

    override var enabled: Boolean = true
    var showGrid = false
    private val orbitalCamera = OrbitalCamera(eye, lookAt)
    private val orbitalControls = OrbitalControls(orbitalCamera)
    private var lastSeconds: Double = -1.0

    private val grid = vertexBuffer(
            vertexFormat {
                position(3)
            }
            , 4 * 21).apply {
        put {
            for (x in -10..10) {
                write(Vector3(x.toDouble(), 0.0, -10.0))
                write(Vector3(x.toDouble(), 0.0, 10.0))
                write(Vector3(-10.0, 0.0, x.toDouble()))
                write(Vector3(10.0, 0.0, x.toDouble()))
            }
        }
   }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (lastSeconds == -1.0) lastSeconds = program.seconds

        val delta = program.seconds - lastSeconds
        lastSeconds = program.seconds
        orbitalCamera.update(delta)

        drawer.background(ColorRGBa.BLACK)
        drawer.perspective(fov, program.window.size.x / program.window.size.y, 0.1, 1000.0)
        drawer.view = orbitalCamera.viewMatrix()

        if (showGrid) {
            drawer.isolated {
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = ColorRGBa.WHITE
                drawer.vertexBuffer(grid, DrawPrimitive.LINES)

                // Axis cross
                drawer.fill = ColorRGBa.RED
                drawer.lineSegment(Vector3.ZERO, Vector3.UNIT_X)

                drawer.fill = ColorRGBa.GREEN
                drawer.lineSegment(Vector3.ZERO, Vector3.UNIT_Y)

                drawer.fill = ColorRGBa.BLUE
                drawer.lineSegment(Vector3.ZERO, Vector3.UNIT_Z)
            }
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.isolated {
            drawer.view = Matrix44.IDENTITY
            drawer.ortho()
        }
    }

    override fun setup(program: Program) {
        orbitalControls.setup(program)
    }
}