package org.openrndr.internal.nullgl

import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.DepthBuffer
import org.openrndr.draw.DepthFormat
import org.openrndr.draw.Session

class DepthBufferNullGL(override val width: Int, override val height: Int, override val format: DepthFormat, override val multisample: BufferMultisample, override val session: Session?) : DepthBuffer {

    override fun resolveTo(target: DepthBuffer) {

    }

    override fun copyTo(target: DepthBuffer) {

    }

    override fun destroy() {

    }

    override fun bind(textureUnit: Int) {

    }
}