package org.openrndr.filter.color

import org.openrndr.draw.*
import org.openrndr.filter.filterFragmentUrl

/**
 * Converts from sRGB to linear RGB
 */
class Linearize : Filter(filterShaderFromUrl(filterFragmentUrl("color/linearize.frag")))

/**
 * Converts from linear RGB to sRGB
 */
class Delinearize : Filter(filterShaderFromUrl(filterFragmentUrl("color/delinearize.frag")))

/**
 * Converts from linear RGB to HybridLogGamma
 */
class HybridLogGamma : Filter(filterShaderFromUrl(filterFragmentUrl("color/hybrid-log-gamma.frag")))

val hybridLogGamma by lazy { HybridLogGamma().apply { untrack() } }
val delinearize by lazy { Delinearize().apply { untrack() } }
val linearize by lazy { Linearize().apply { untrack() } }
