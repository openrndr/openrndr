package org.openrndr.draw

import org.openrndr.internal.Driver


enum class ShaderType {
    VERTEX,
    TESSELLATION_CONTROL,
    TESSELLATION_EVALUATION,
    GEOMETRY,
    FRAGMENT,
    COMPUTE
}

interface Shader : ShaderImageBindings, ShaderUniforms, ShaderTextureBindings, ShaderBufferBindings {
    val session: Session?
    val types: Set<ShaderType>

    @Suppress("unused")
    companion object {
        fun createFromCode(
            vsCode: String,
            tcsCode: String? = null,
            tesCode: String? = null,
            gsCode: String? = null,
            fsCode: String,
            name: String,
            session: Session? = Session.active
        ): Shader {
            val shader = Driver.instance.createShader(vsCode, tcsCode, tesCode, gsCode, fsCode, name, session)
            session?.track(shader)
            return shader
        }
    }

    fun begin()
    fun end()

    fun hasUniform(name: String): Boolean

    fun createBlock(blockName: String): UniformBlock?
    fun blockLayout(blockName: String): UniformBlockLayout?
    fun block(blockName: String, block: UniformBlock)


    fun destroy()
}
