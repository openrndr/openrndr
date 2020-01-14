package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.math.*

interface Shader {
    val session: Session?

    @Suppress("unused")
    companion object {
        fun createFromUrls(vsUrl: String, fsUrl: String, session: Session? = Session.active): Shader {
            val vsCode = codeFromURL(vsUrl)
            val fsCode = codeFromURL(fsUrl)
            val shader = Driver.instance.createShader(vsCode, fsCode)
            session?.track(shader)
            return shader
        }

        fun createFromCode(vsCode: String, fsCode: String, session: Session? = Session.active): Shader {
            val shader = Driver.instance.createShader(vsCode, fsCode)
            session?.track(shader)
            return shader
        }
    }

    fun begin()
    fun end()

    fun hasUniform(name: String): Boolean

    fun createBlock(blockName: String): UniformBlock
    fun blockLayout(blockName: String): UniformBlockLayout?
    fun block(blockName: String, block: UniformBlock)

    fun uniform(name: String, value: Matrix33)
    fun uniform(name: String, value: Matrix44)

    fun uniform(name: String, value: ColorRGBa)
    fun uniform(name: String, value: Vector4)
    fun uniform(name: String, value: Vector3)
    fun uniform(name: String, value: Vector2)
    fun uniform(name: String, x: Float, y: Float, z: Float, w: Float)
    fun uniform(name: String, x: Float, y: Float, z: Float)
    fun uniform(name: String, x: Float, y: Float)
    fun uniform(name: String, value: Double)
    fun uniform(name: String, value: Float)
    fun uniform(name: String, value: Int)
    fun uniform(name: String, value: Boolean)

    fun uniform(name: String, value: Array<Vector4>)
    fun uniform(name: String, value: Array<Vector3>)
    fun uniform(name: String, value: Array<Vector2>)
    fun uniform(name: String, value: FloatArray)

    fun destroy()
}
