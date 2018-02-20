package org.openrndr.filter.antialias

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.filter.filterFragmentCode

class FXAA : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/color-burn.frag"))) {


    var lumaThreshold: Double = 0.5
        set(value) {
            parameters["lumaTreshold"] = value; field = value
        }
    var maxSpan: Double = 8.0
        set(value) {
            parameters["maxSpan"] = value; field = value
        }

    var directionReduceMultiplier: Double = 0.0
        set(value) {
            parameters["directionReduceMultiplier"] = value; field = value
        }

    var directionReduceMinimum: Double = 0.0
        set(value) {
            parameters["directionReduceMinimum"] = value; field = value
        }


    init {
        lumaThreshold = 0.5
        maxSpan = 8.0
        directionReduceMinimum = 0.0
        directionReduceMultiplier = 0.0
    }
}