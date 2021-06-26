package org.openrndr.filter.color

import org.openrndr.draw.*
import org.openrndr.filter.filter_delinearize
import org.openrndr.filter.filter_hybrid_log_gamma
import org.openrndr.filter.filter_linearize
import org.openrndr.filter.mppFilterShader
/**
 * Converts from sRGB to linear RGB
 */
class Linearize : Filter(mppFilterShader(filter_linearize,"linearize"))

/**
 * Converts from linear RGB to sRGB
 */
class Delinearize : Filter(mppFilterShader(filter_delinearize, "delinearize"))

/**
 * Converts from linear RGB to HybridLogGamma
 */
class HybridLogGamma : Filter(mppFilterShader(filter_hybrid_log_gamma,"hybrid-log-gamma"))

val hybridLogGamma by lazy { HybridLogGamma().apply { untrack() } }
val delinearize by lazy { Delinearize().apply { untrack() } }
val linearize by lazy { Linearize().apply { untrack() } }
