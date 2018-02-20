package org.openrndr.filter.blur

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.filter.filterFragmentCode

class GaussianBlur : Filter(Shader.createFromCode(Filter.filterVertexCode,
        filterFragmentCode("blur/approximate-gaussian-blur.frag"))) {
    var window: Int = 5
        set(value) {
            parameters["window"] = value; field = value
        }

    var spread: Double = 1.0
        set(value) {
            parameters["spread"] = value; field = value
        }

    var sigma: Double = 1.0
        set (value) {
            parameters["sigma"] = value; field = value
        }

    var gain: Double = 1.0
        set(value) {
            parameters["gain"] = value; field = value
        }


}