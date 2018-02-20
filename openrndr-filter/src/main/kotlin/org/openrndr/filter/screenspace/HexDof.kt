@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.openrndr.filter.screenspace

import org.openrndr.draw.*
import org.openrndr.filter.filterFragmentCode
import org.openrndr.math.Vector2


class HexDof : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("screenspace/hex-dof-pass-1.frag"))) {
    private var pass1 = Pass1()
    private var pass2 = Pass2()
    private var vertical: ColorBuffer? = null
    private var diagonal: ColorBuffer? = null

    var samples = 20

    class Pass1 : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("screenspace/hex-dof-pass-1.frag"))) {
        var image:Int by parameters
        init {
            image = 0
        }
    }

    class Pass2 : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("screenspace/hex-dof-pass-2.frag"))) {
        var vertical:Int by parameters
        var diagonal:Int by parameters
        var original:Int by parameters
        init {
            vertical = 0
            diagonal = 1
            original = 2
        }
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {

        if (vertical != null && (vertical!!.width != target[0].width || vertical!!.height != target[0].height)) {
            vertical!!.destroy()
            vertical = null
            diagonal!!.destroy()
            diagonal = null
        }

        if (vertical == null) {
            vertical = ColorBuffer.create(target[0].width, target[0].height, ColorFormat.RGBa, ColorType.FLOAT16)
            diagonal = ColorBuffer.create(target[0].width, target[0].height, ColorFormat.RGBa, ColorType.FLOAT16)
        }

        source[0].filter(MinifyingFilter.LINEAR, MagnifyingFilter.LINEAR) // image


        vertical!!.filter(MinifyingFilter.LINEAR, MagnifyingFilter.LINEAR)
        diagonal!!.filter(MinifyingFilter.LINEAR, MagnifyingFilter.LINEAR)

        pass1.parameters["samples"] = samples
        pass1.parameters["vertical"] = Vector2(Math.cos(Math.PI / 2), Math.sin(Math.PI / 2))
        pass1.parameters["diagonal"] = Vector2(Math.cos(-Math.PI / 6), Math.sin(-Math.PI / 6))
        pass1.apply(source[0], arrayOf(vertical!!, diagonal!!))
        pass2.parameters["samples"]  = samples
        pass2.parameters["direction0"] = Vector2(Math.cos(-Math.PI / 6), Math.sin(-Math.PI / 6))
        pass2.parameters["direction1"] = Vector2(Math.cos(-5 * Math.PI / 6), Math.sin(-5 * Math.PI / 6))
        pass2.apply(arrayOf(vertical!!, diagonal!!, source[0]), target)
    }
}

class PositionToCoc : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("screenspace/position-to-coc.frag"))) {
    var minCoc: Double by parameters
    var maxCoc: Double by parameters

    var aperture: Double by parameters
    var zFar: Double by parameters
    var zNear:Double by parameters
    var focalPlane:Double by parameters
    var focalLength:Double by parameters
    var exposure:Double by parameters

    var position:Int by parameters

    init {
        minCoc = 2.0
        maxCoc = 20.0
        position = 1

        focalLength = 2.0
        focalPlane = 4.0
        zNear = 0.1
        zFar = 1400.0
        aperture = 1.0

    }
}

class CocBlur: Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("screenspace/coc-blur.frag"))) {
    var window:Int by parameters
    var sigma:Double by parameters

    init {
        window = 4
        sigma = 4.0
    }

}

class VelocityBlur: Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("screenspace/velocity-blur.frag"))) {
    var window:Int by parameters

    var velocity: Int by parameters
    init {
        window = 10
        velocity = 1
    }

}

class IterativeVelocityBlur: Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("screenspace/iterative-velocity-blur.frag"))) {

    var iterations:Int = 10

    var step:Int by parameters
    var velocity: Int by parameters
    init {
        velocity = 1
    }

    var intermediate:ColorBuffer? = null

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        intermediate?.let {
            if (it.width != target[0].width || it.height != target[0].height) {
                it.destroy()
                intermediate = null
            }
        }

        if (intermediate == null) {
            intermediate = ColorBuffer.create(target[0].width, target[0].height, target[0].format, target[0].type)
        }

        val v = source[1]
        step = 0
        super.apply(arrayOf(source[0], v), arrayOf(intermediate!!))

        step++
        super.apply(arrayOf(intermediate!!, v), target)
        step++
        super.apply(arrayOf(target[0], v), arrayOf(intermediate!!))
        step++
        super.apply(arrayOf(intermediate!!, v), target)
        step++
    }

}