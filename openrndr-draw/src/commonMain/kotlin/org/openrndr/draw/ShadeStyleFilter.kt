package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.internal.shareGroupID
import org.openrndr.shape.Rectangle

/**
 * The shade style managers of shade style filters, one per [share group][Driver.shareGroupID]
 */
private val shadeStyleManagers = mutableMapOf<Long, ShadeStyleManager>()

private val shadeStyleManager: ShadeStyleManager
    get() = shadeStyleManagers.getOrPut(Driver.instance.shareGroupID) {
        ShadeStyleManager.fromGenerators(
            "shade-style-filter",
            vsGenerator = Driver.instance.shaderGenerators::filterVertexShader,
            fsGenerator = Driver.instance.shaderGenerators::filterFragmentShader
        )
    }

/**
 * A [Filter] that uses the [ShadeStyle] interface and language
 * @param shadeStyle the [ShadeStyle] that is applied as a filter
 */
class ShadeStyleFilter(val shadeStyle: ShadeStyle) : Filter(null), StyleParameters by shadeStyle {
    var fill: ColorRGBa by parameters

    init {
        fill = ColorRGBa.WHITE
        shadeStyle.parameter("fill", fill)
    }
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        shadeStyle.parameter("fill", fill)
        shadeStyle.textureBaseIndex = source.size

        for ((index, buffer) in source.withIndex()) {
            shadeStyle.parameter("input$index", buffer)
        }
        val shader = shadeStyleManager.shader(shadeStyle, format)
        val realFilter = Filter(shader)
        realFilter.apply(source, target, clip)
    }
}


open class ShadeStyleFilter1to1(val shadeStyle: ShadeStyle) : Filter1to1(null), StyleParameters by shadeStyle {
    var fill: ColorRGBa by parameters

    init {
        fill = ColorRGBa.WHITE
        shadeStyle.parameter("fill", fill)
    }
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        shadeStyle.parameter("fill", fill)
        shadeStyle.textureBaseIndex = source.size

        for ((index, buffer) in source.withIndex()) {
            shadeStyle.parameter("input$index", buffer)
        }
        val shader = shadeStyleManager.shader(shadeStyle, format)
        val realFilter = Filter1to1(shader)
        realFilter.apply(source, target, clip)
    }
}


class ShadeStyleFilter2to1(val shadeStyle: ShadeStyle) : Filter2to1(null), StyleParameters by shadeStyle {
    var fill: ColorRGBa by parameters

    init {
        fill = ColorRGBa.WHITE
        shadeStyle.parameter("fill", fill)
    }
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        shadeStyle.parameter("fill", fill)
        shadeStyle.textureBaseIndex = source.size
        for ((index, buffer) in source.withIndex()) {
            shadeStyle.parameter("input$index", buffer)
        }
        val shader = shadeStyleManager.shader(shadeStyle, format)
        val realFilter = Filter2to1(shader)
        realFilter.apply(source, target, clip)
    }
}

