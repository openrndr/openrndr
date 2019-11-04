package org.openrndr.filter.blend

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.filter.filterFragmentCode

class ColorBurn : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/color-burn.frag")))
class ColorDodge : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/color-dodge.frag")))
class Darken : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/darken.frag")))
class HardLight : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/hard-light.frag")))
class Lighten : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/lighten.frag")))
class Multiply : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/multiply.frag")))
class Normal : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/normal.frag")))
class Overlay : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/overlay.frag")))
class Screen : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/screen.frag")))

class MultiplyContrast : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/multiply-contrast.frag")))

class Passthrough : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/passthrough.frag")))
class Add : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/add.frag")))
class Subtract : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/subtract.frag")))

val multiply: Multiply by lazy { Multiply().apply { untrack() } }
val passthrough: Passthrough by lazy { Passthrough().apply { untrack() } }
val add: Add by lazy { Add().apply { untrack() } }
val subtract: Subtract by lazy { Subtract().apply { untrack() } }