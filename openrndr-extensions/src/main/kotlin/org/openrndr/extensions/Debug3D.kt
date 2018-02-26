package org.openrndr.extensions


import org.openrndr.Extension
import org.openrndr.KeyboardModifier
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*
import org.openrndr.math.transforms.translate

private class FirstPersonCamera {

    var viewMatrix = Matrix44.IDENTITY
    var cameraQuat = Quaternion.IDENTITY

    var position = Vector3.ZERO
    var keyPitch = 0.0
    var keyYaw = 0.0
    var keyRoll = 0.0

    var lastMousePosition = Vector2(0.0, 0.0)


    val forward: Vector3
        get() {
            val mat = cameraQuat.matrix.transposed
            return mat[2].xyz
        }
    val up: Vector3
        get() {
            val mat = cameraQuat.matrix.transposed
            return mat[1].xyz
        }

    fun mouseScrolled(event: Program.Mouse.MouseEvent) {
        val mat = cameraQuat.matrix.transposed
        val foward = mat[2].xyz
        val strafe = mat[0].xyz
        val up = mat[1].xyz

        position += if (KeyboardModifier.SHIFT in event.modifiers) {
            up * event.rotation.y * 10.0
        } else {
            foward * event.rotation.y * 10.0
        }
        position += strafe * event.rotation.x * 3.0

        update()
    }

    fun mouseMoved(event: Program.Mouse.MouseEvent) {

        val delta = event.position - lastMousePosition

        if (event.modifiers.contains(KeyboardModifier.SHIFT))

            keyYaw = delta.x * 0.01
        else {
            keyPitch = delta.x * 0.01
            keyRoll = delta.y * 0.01
        }
        lastMousePosition = event.position

        update()
    }

    fun update() {
        val keyQuat = fromAngles(keyPitch, keyRoll, keyYaw)
        cameraQuat = keyQuat * cameraQuat
        keyPitch = 0.0
        keyYaw = 0.0
        keyRoll = 0.0

        viewMatrix = cameraQuat.matrix * translate(position * -1.0)
    }

}

class Debug3D : Extension {


    private val firstPersonCamera = FirstPersonCamera()

    private val grid = vertexBuffer(
            vertexFormat {
                position(3)
            }
            , 4 * 21).apply {
        put {
            for (x in -10 .. 10) {
                write(Vector3(x.toDouble(), 0.0, -10.0))
                write(Vector3(x.toDouble(), 0.0, 10.0))

                write(Vector3(-10.0, 0.0, x.toDouble()))
                write(Vector3(10.0, 0.0, x.toDouble()))

            }
        }

    }

    override fun setup(program: Program) {
        program.mouse.scrolled.listen { firstPersonCamera.mouseScrolled(it) }
        program.mouse.moved.listen { firstPersonCamera.mouseMoved(it) }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        drawer.background(ColorRGBa.BLACK)
        drawer.perspective(90.0, program.window.size.x / program.window.size.y, 0.1, 1000.0)
        drawer.view = firstPersonCamera.viewMatrix

        drawer.isolated {
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = ColorRGBa.WHITE
            drawer.vertexBuffer(listOf(grid), DrawPrimitive.LINES)
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.isolated {
            drawer.view = Matrix44.IDENTITY
            drawer.ortho()
        }
    }


}