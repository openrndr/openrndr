package org.openrndr.filter.antialias

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.filter.filterFragmentCode

class FXAA : Filter(Shader.createFromCode(Filter.filterTriangleVertexCode, filterFragmentCode("antialias/fxaa.frag"))) {

    var lumaThreshold: Double by parameters
    var maxSpan: Double by parameters
    var directionReduceMultiplier:Double by parameters

    var directionReduceMinimum:Double by parameters

    init {
        lumaThreshold = 0.5
        maxSpan = 8.0
        directionReduceMinimum = 0.0
        directionReduceMultiplier = 0.0
    }
}