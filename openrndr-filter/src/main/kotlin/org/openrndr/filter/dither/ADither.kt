package org.openrndr.filter.dither

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.filter.filterFragmentCode

class ADither: Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("dither/a-dither.frag"))) {
    var pattern: Int
        set(value) {
            parameters["pattern"] = value
        }
        get() = parameters["pattern"] as Int

    var levels: Int
        set(value) {
            parameters["levels"] = value;
        }
        get() = parameters["levels"] as Int

    init {
        levels = 4
        pattern = 3
    }
}