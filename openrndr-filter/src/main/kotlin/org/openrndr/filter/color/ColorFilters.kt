package org.openrndr.filter.color

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.filter.filterFragmentCode

class Linearize: Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/linearize.frag")))
class Delinearize: Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/delinearize.frag")))
class TonemapUncharted2: Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/tonemap-uncharted2.frag")))

class ColorMix: Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/color-mix.frag")))

class HybridLogGamma:  Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/hybrid-log-gamma.frag")))

val hybridLogGamma by lazy { HybridLogGamma() }
val delinearize by lazy { Delinearize() }
val linearize by lazy { Linearize() }
val tonemapUncharted2 by lazy {TonemapUncharted2()}