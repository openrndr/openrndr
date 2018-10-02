package org.openrndr.filter.blur

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.filter.filterFragmentCode

class GaussianBlur : Filter(Shader.createFromCode(Filter.filterVertexCode,
        filterFragmentCode("blur/gaussian-blur.frag"))) {

    var window: Int by parameters
    var spread: Double by parameters
    var sigma: Double by parameters
    var gain: Double by parameters

    init {
        window = 5
        spread = 1.0
        sigma = 1.0
        gain = 1.0
    }


}