package org.openrndr.filter.blur

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.filter.filterFragmentCode

class HashBlur : Filter(Shader.createFromCode(Filter.filterTriangleVertexCode,
        filterFragmentCode("blur/hash-blur.frag"))) {

    var radius:Double by parameters
    var time:Double by parameters
    var samples:Int by parameters
    var gain:Double by parameters

    init {
        radius = 5.0
        time = 0.0
        samples = 30
        gain = 1.0
    }
}