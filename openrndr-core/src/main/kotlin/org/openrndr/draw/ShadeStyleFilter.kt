package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver

private val shadeStyleManager by lazy {
    ShadeStyleManager.fromGenerators(
            Driver.instance.shaderGenerators::filterVertexShader,
            Driver.instance.shaderGenerators::filterFragmentShader
    )
}

/**
 * A [Filter] that uses the [ShadeStyle] interface and language
 * @param shadeStyle the [ShadeStyle] that is applied as a filter
 */
class ShadeStyleFilter(val shadeStyle: ShadeStyle) : Filter() {
    var fill: ColorRGBa by parameters

    init {
        fill = ColorRGBa.WHITE
        shadeStyle.parameter("fill", fill)
    }

    val shader = shadeStyleManager.shader(shadeStyle, format)

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        shadeStyle.parameter("fill", fill)
        for ((index, buffer) in source.withIndex()) {
            shadeStyle.parameter("input$index", buffer)
        }
        val shader = shadeStyleManager.shader(shadeStyle, format)
        val realFilter = Filter(shader)
        realFilter.apply(source, target)
    }
}


