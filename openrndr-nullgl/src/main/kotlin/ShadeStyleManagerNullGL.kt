package org.openrndr.internal.nullgl

import org.openrndr.draw.*

class ShadeStyleManagerNullGL(name:String) : ShadeStyleManager(name) {
    override fun shader(style: ShadeStyle?, vertexFormats: List<VertexFormat>, instanceFormats: List<VertexFormat>): Shader {
        return ShaderNullGL(Session.active)
    }
}