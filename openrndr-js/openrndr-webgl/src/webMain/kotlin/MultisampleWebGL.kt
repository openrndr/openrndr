@file:OptIn(ExperimentalWasmJsInterop::class)

package org.openrndr.webgl

import org.openrndr.draw.BufferMultisample
import web.gl.WebGL2RenderingContext as GL

private fun maxSamples(context: GL): Int = js("context.getParameter(context.MAX_SAMPLES)")

/**
 * The number of samples of multisampled renderbuffers, limited to what the context supports
 */
internal fun BufferMultisample.SampleCount.samples(context: GL): Int = minOf(sampleCount, maxSamples(context))
