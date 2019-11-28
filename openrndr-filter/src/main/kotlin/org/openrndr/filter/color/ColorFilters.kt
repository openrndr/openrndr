package org.openrndr.filter.color

import org.openrndr.draw.*
import org.openrndr.filter.filterFragmentCode

class Linearize : Filter(Shader.createFromCode(Filter.filterTriangleVertexCode, filterFragmentCode("color/linearize.frag")))
class Delinearize : Filter(Shader.createFromCode(Filter.filterTriangleVertexCode, filterFragmentCode("color/delinearize.frag")))
class ColorMix : Filter(Shader.createFromCode(Filter.filterTriangleVertexCode, filterFragmentCode("color/color-mix.frag")))
class HybridLogGamma : Filter(Shader.createFromCode(Filter.filterTriangleVertexCode, filterFragmentCode("color/hybrid-log-gamma.frag")))

class ColorLookup(lookup: ColorBuffer) : Filter(Shader.createFromCode(Filter.filterTriangleVertexCode, filterFragmentCode("color/color-lookup.frag"))) {
    var lookup: ColorBuffer by parameters
    var noiseGain: Double by parameters
    var seed: Double by parameters

    init {
        this.lookup = lookup
        this.noiseGain = 0.0
        this.seed = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        lookup.filter(MinifyingFilter.LINEAR, MagnifyingFilter.LINEAR)
        super.apply(source, target)
    }
}

val hybridLogGamma by lazy { HybridLogGamma().apply { untrack() } }
val delinearize by lazy { Delinearize().apply { untrack() } }
val linearize by lazy { Linearize().apply { untrack() } }
