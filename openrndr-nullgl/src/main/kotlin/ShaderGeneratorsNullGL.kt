package org.openrndr.internal.nullgl

import org.openrndr.draw.ShadeStructure
import org.openrndr.internal.ShaderGenerators

class ShaderGeneratorsNullGL : ShaderGenerators {
    override fun vertexBufferFragmentShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun vertexBufferVertexShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun imageFragmentShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun imageVertexShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun imageArrayTextureFragmentShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun imageArrayTextureVertexShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun pointFragmentShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun pointVertexShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun circleFragmentShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun circleVertexShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun fontImageMapFragmentShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun fontImageMapVertexShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun rectangleFragmentShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun rectangleVertexShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun expansionFragmentShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun expansionVertexShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun fastLineFragmentShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun fastLineVertexShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun meshLineFragmentShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun meshLineVertexShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun filterVertexShader(shadeStructure: ShadeStructure): String {
        return ""
    }

    override fun filterFragmentShader(shadeStructure: ShadeStructure): String {
        return ""
    }
}