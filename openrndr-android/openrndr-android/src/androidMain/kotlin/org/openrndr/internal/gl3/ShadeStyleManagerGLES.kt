package org.openrndr.internal.gl3

import android.opengl.GLES32
import org.openrndr.draw.*

class ShadeStyleManagerGLES(name:String) : ShadeStyleManager(name) {
    override fun shader(style: ShadeStyle?, vertexFormats: List<VertexFormat>, instanceFormats: List<VertexFormat>): Shader {
        return ShaderGLES(
            programId = GLES32.glCreateProgram(),
            session = Session.active)
    }
}