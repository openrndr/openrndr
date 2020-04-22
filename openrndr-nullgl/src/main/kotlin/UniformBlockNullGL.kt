package org.openrndr.internal.nullgl

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.UniformBlock
import org.openrndr.draw.UniformBlockLayout
import org.openrndr.math.*

class UniformBlockNullGL(override val layout: UniformBlockLayout) : UniformBlock {

    override val dirty: Boolean = true

    override fun uniform(name: String, value: Array<Float>) {

    }

    override fun uniform(name: String, value: Float) {

    }

    override fun uniform(name: String, value: Vector2) {

    }

    override fun uniform(name: String, value: Vector3) {

    }

    override fun uniform(name: String, value: Vector4) {

    }

    override fun uniform(name: String, value: ColorRGBa) {

    }

    override fun uniform(name: String, value: Matrix44) {

    }

    override fun uniform(name: String, value: Matrix55) {

    }

    override fun uniform(name: String, value: Array<Vector2>) {

    }

    override fun uniform(name: String, value: Array<Vector3>) {

    }

    override fun uniform(name: String, value: Array<Vector4>) {

    }

    override fun upload() {

    }
}