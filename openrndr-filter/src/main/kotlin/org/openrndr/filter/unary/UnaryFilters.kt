package org.openrndr.filter.unary

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.filter.filterFragmentCode

class SubtractConstant: Filter(Shader.createFromCode(Filter.filterTriangleVertexCode, filterFragmentCode("unary/subtract-constant.frag"))) {

    var constant:ColorRGBa by parameters
    init {
        constant = ColorRGBa(1.0, 1.0, 1.0, 0.0)
    }

}
