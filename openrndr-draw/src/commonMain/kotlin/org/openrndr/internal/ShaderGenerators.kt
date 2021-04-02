package org.openrndr.internal

import org.openrndr.draw.ShadeStructure


/**
 *  built-in shader generators
 */
interface ShaderGenerators {
    fun vertexBufferFragmentShader(shadeStructure: ShadeStructure): String
    fun vertexBufferVertexShader(shadeStructure: ShadeStructure): String

    fun imageFragmentShader(shadeStructure: ShadeStructure): String
    fun imageVertexShader(shadeStructure: ShadeStructure): String

    fun imageArrayTextureFragmentShader(shadeStructure: ShadeStructure): String
    fun imageArrayTextureVertexShader(shadeStructure: ShadeStructure): String

    fun pointFragmentShader(shadeStructure: ShadeStructure): String
    fun pointVertexShader(shadeStructure: ShadeStructure): String

    fun circleFragmentShader(shadeStructure: ShadeStructure): String
    fun circleVertexShader(shadeStructure: ShadeStructure): String

    fun fontImageMapFragmentShader(shadeStructure: ShadeStructure): String
    fun fontImageMapVertexShader(shadeStructure: ShadeStructure): String

    fun rectangleFragmentShader(shadeStructure: ShadeStructure): String
    fun rectangleVertexShader(shadeStructure: ShadeStructure): String

    fun expansionFragmentShader(shadeStructure: ShadeStructure): String
    fun expansionVertexShader(shadeStructure: ShadeStructure): String

    fun fastLineFragmentShader(shadeStructure: ShadeStructure): String
    fun fastLineVertexShader(shadeStructure: ShadeStructure): String

    fun meshLineFragmentShader(shadeStructure: ShadeStructure): String
    fun meshLineVertexShader(shadeStructure: ShadeStructure): String

    fun filterVertexShader(shadeStructure: ShadeStructure): String
    fun filterFragmentShader(shadeStructure: ShadeStructure): String
}
