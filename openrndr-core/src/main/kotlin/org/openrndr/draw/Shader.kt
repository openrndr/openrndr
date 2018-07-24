package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

interface Shader {

    @Suppress("unused")
    companion object {
        fun createFromUrls(vsUrl: String, fsUrl: String): Shader {
            val vsCode = codeFromURL(vsUrl)
            val fsCode = codeFromURL(fsUrl)
            return Driver.instance.createShader(vsCode, fsCode)
        }

        fun createFromCode(vsCode: String, fsCode: String): Shader {
            return Driver.instance.createShader(vsCode, fsCode)
        }
    }

    fun begin()
    fun end()

    fun hasUniform(name: String): Boolean

    fun createBlock(blockName: String): UniformBlock
    fun blockLayout(blockName: String): UniformBlockLayout?
    fun block(blockName: String, block: UniformBlock)


    fun uniform(name: String, value: Matrix44)
    fun uniform(name: String, value: ColorRGBa)
    fun uniform(name: String, value: Vector4)
    fun uniform(name: String, value: Vector3)
    fun uniform(name: String, value: Vector2)
    fun uniform(name: String, value: Double)
    fun uniform(name: String, value: Float)
    fun uniform(name: String, value: Int)

    fun uniform(name: String, value: Array<Vector4>)
    fun uniform(name: String, value: Array<Vector3>)
    fun uniform(name: String, value: Array<Vector2>)
    fun uniform(name: String, value: FloatArray)

}
