@file:Suppress("unused")

package org.openrndr.filter.screenspace

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.filter.filterFragmentCode
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector4
import org.openrndr.math.mix

class Ssao : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("screenspace/ssao.frag"))) {
    var projection: Matrix44 by parameters
    var colors: Int by parameters
    var positions: Int by parameters
    var normals: Int by parameters

    var radius: Double by parameters

    init {
        val poissonSamples = Array(64) { Vector4.ZERO }
        for (i in 0..63) {
            var scale = i / 63.0
            scale = mix(0.1, 1.0, scale * scale)
            poissonSamples[i] = Vector4(Math.random() * 2 - 1, Math.random() * 2 - 1, Math.random(), 0.0).normalized * scale
        }
        parameters["poissonSamples"] = poissonSamples
        colors = 0
        positions = 1
        normals = 2
        projection = Matrix44.IDENTITY
        radius = 4.0
    }
}

class Sslr : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("screenspace/sslr.frag"))) {
    var projection: Matrix44 by parameters
    var colors: Int by parameters
    var positions: Int by parameters
    var normals: Int by parameters

    var jitterOriginGain: Double by parameters
    var iterationLimit: Int by parameters
    var distanceLimit: Double by parameters
    var gain: Double by parameters
    var borderWidth: Double by parameters

    init {
        colors = 0
        positions = 1
        normals = 2
        projection = Matrix44.IDENTITY

        distanceLimit = 1280.0/4.0
        iterationLimit = 64
        jitterOriginGain = 0.1

        gain = 0.25
        borderWidth = 130.0
    }
}

class OcclusionBlur : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("screenspace/occlusion-blur.frag"))) {
    var occlusion: Int by parameters
    var positions: Int by parameters
    var normals: Int by parameters

    init {
        occlusion = 0
        positions = 1
        normals = 2
    }
}