package org.openrndr.filter.transforms

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.filter.filterFragmentCode

class FlipVertically: Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("transform/flip-vertically.frag")))

val flipVertically: FlipVertically by lazy { FlipVertically() }