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
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) =
        withInputMipmaps(source) { inputs -> applyShadeStyle(shadeStyle, fill, inputs, target, clip) }
}


open class ShadeStyleFilter1to1(val shadeStyle: ShadeStyle) : Filter1to1(null), StyleParameters by shadeStyle {
    var fill: ColorRGBa by parameters

    init {
        fill = ColorRGBa.WHITE
        shadeStyle.parameter("fill", fill)
    }
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) =
        withInputMipmaps(source) { inputs -> applyShadeStyle(shadeStyle, fill, inputs, target, clip) }
}


class ShadeStyleFilter2to1(val shadeStyle: ShadeStyle) : Filter2to1(null), StyleParameters by shadeStyle {
    var fill: ColorRGBa by parameters

    init {
        fill = ColorRGBa.WHITE
        shadeStyle.parameter("fill", fill)
    }
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) =
        withInputMipmaps(source) { inputs -> applyShadeStyle(shadeStyle, fill, inputs, target, clip) }
}

/** the names of the inputs of shade style filters */
private val inputNames = Array(8) { "input$it" }

/**
 * Applies [shadeStyle] as a filter of [source], which are its inputs `input0` to `input<n>`, with the render target,
 * padding and depth buffer of this filter
 */
private fun Filter.applyShadeStyle(
    shadeStyle: ShadeStyle,
    fill: ColorRGBa,
    source: Array<ColorBuffer>,
    target: Array<ColorBuffer>,
    clip: Rectangle?
) {
    shadeStyle.parameter("fill", fill)
    shadeStyle.textureBaseIndex = source.size
    for (index in source.indices) {
        shadeStyle.parameter(inputNames.getOrNull(index) ?: "input$index", source[index])
    }
    val shader = shadeStyleManager.shader(shadeStyle, filterQuadFormat)
    applyShader(shader, source, target, clip, dispatchParameters = false)
}
