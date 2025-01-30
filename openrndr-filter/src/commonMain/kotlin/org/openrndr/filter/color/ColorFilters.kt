package org.openrndr.filter.color

import org.openrndr.draw.*
import org.openrndr.filter.filter_copy
import org.openrndr.filter.filter_delinearize
import org.openrndr.filter.filter_hybrid_log_gamma
import org.openrndr.filter.filter_linearize

/**
 * A specialized filter that performs a direct 1-to-1 copy of image data.
 * It utilizes a shader for processing, with the shader being generated
 * from predefined code using the `filterShaderFromCode` function.
 *
 * The `Copy` class inherits from `Filter1to1`, which is a base class
 * for filters that map one input to one output using a shader.
 */
class Copy : Filter1to1(filterShaderFromCode(filter_copy,"copy"))

/**
 * Converts from sRGB to linear RGB
 */
class Linearize : Filter1to1(filterShaderFromCode(filter_linearize,"linearize"))

/**
 * Converts from linear RGB to sRGB
 */
class Delinearize : Filter1to1(filterShaderFromCode(filter_delinearize, "delinearize"))

/**
 * Converts from linear RGB to HybridLogGamma
 */
class HybridLogGamma : Filter1to1(filterShaderFromCode(filter_hybrid_log_gamma,"hybrid-log-gamma"))

val hybridLogGamma by lazy { HybridLogGamma().apply { untrack() } }
val delinearize by lazy { Delinearize().apply { untrack() } }
val linearize by lazy { Linearize().apply { untrack() } }
val copy by lazy { Copy().apply { untrack() }}