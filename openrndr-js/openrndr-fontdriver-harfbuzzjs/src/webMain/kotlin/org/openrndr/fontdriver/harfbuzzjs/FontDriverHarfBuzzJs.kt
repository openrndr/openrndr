@file:OptIn(ExperimentalWasmJsInterop::class)

package org.openrndr.fontdriver.harfbuzzjs

import js.typedarrays.Uint8Array
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.FaceMaster
import org.openrndr.draw.font.internal.FontDriver

/**
 * A [FontDriver] for browsers and Node.js, backed by harfbuzzjs. Create it with [FontDriverHarfBuzzJs.create], which
 * loads the harfbuzzjs WebAssembly module.
 *
 * [loadFace] reads font files synchronously, with a synchronous request in browsers, [loadFaceSuspend] fetches them
 * without blocking.
 */
class FontDriverHarfBuzzJs(val harfBuzz: HarfBuzzModule) : FontDriver {

    override fun loadFace(fileOrUrl: String): FaceHarfBuzzJs = loadFace(readBytesSync(fileOrUrl))

    override suspend fun loadFaceSuspend(fileOrUrl: String): FaceHarfBuzzJs = loadFace(readBytes(fileOrUrl))

    /**
     * Loads a face from the bytes of a font file
     * @param index the index of the face in a font collection
     */
    fun loadFace(bytes: ByteArray, index: Int = 0): FaceHarfBuzzJs = loadFace(byteArrayToUint8Array(bytes), index)

    private fun loadFace(bytes: Uint8Array<*>, index: Int = 0): FaceHarfBuzzJs {
        val hbFace = hbCreateFace(harfBuzz, bytes, index)

        // variation axes are named like FreeType names them, by the name of the axis in the font
        val axes = hbAxes(hbFace)
        val axisTags = mutableMapOf<String, String>()
        val values = mutableMapOf<String, Double>()
        val ranges = mutableMapOf<String, ClosedFloatingPointRange<Double>>()
        for (i in 0 until hbAxisCount(axes)) {
            val name = hbAxisName(hbFace, axes, i)
            axisTags[name] = hbAxisTag(axes, i)
            values[name] = hbAxisDefault(axes, i)
            ranges[name] = hbAxisMin(axes, i)..hbAxisMax(axes, i)
        }
        val master = FaceMaster(values, ranges)
        val data = HarfBuzzFaceData(harfBuzz, hbFace, axisTags, master)
        return FaceHarfBuzzJs(data, FontDriver.DEFAULT_SIZE_IN_POINTS, master.copy())
    }

    companion object {
        /**
         * Creates the driver, after loading the harfbuzzjs WebAssembly module
         */
        suspend fun create(): FontDriverHarfBuzzJs = FontDriverHarfBuzzJs(loadHarfBuzzModule())
    }
}
