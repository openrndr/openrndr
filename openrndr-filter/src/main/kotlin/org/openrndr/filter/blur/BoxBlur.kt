package org.openrndr.filter.blur

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.draw.colorBuffer
import org.openrndr.filter.filterFragmentCode
import org.openrndr.math.Vector2

class BoxBlur : Filter(Shader.createFromCode(Filter.filterVertexCode,
        filterFragmentCode("blur/box-blur.frag"))) {

    var window: Int by parameters
    var spread: Double by parameters
    var gain: Double by parameters

    var intermediate:ColorBuffer? = null

    init {
        window = 5
        spread = 1.0
        gain = 1.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {

        intermediate?.let {
            if (it.width != target[0].width || it.height != target[0].height) {
                intermediate = null
            }
        }

        if (intermediate == null) {
            intermediate = colorBuffer(target[0].width, target[0].height, target[0].contentScale, target[0].format, target[0].type)
        }

        intermediate?.let {
            parameters["blurDirection"] = Vector2(1.0, 0.0)
            super.apply(source, arrayOf(it))

            parameters["blurDirection"] = Vector2(0.0, 1.0)
            super.apply(arrayOf(it), target)
        }

    }
}