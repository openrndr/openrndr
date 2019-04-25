package org.openrndr.draw

import mu.KotlinLogging
import org.openrndr.internal.Driver

import java.util.*
private val logger = KotlinLogging.logger {}

private val sessionStack = mutableMapOf<Long, Stack<Session>>()

class Session {
    val context = Driver.instance.contextID

    companion object {
        val active: Session
        get() = sessionStack.getOrPut(Driver.instance.contextID) { Stack<Session>().apply { push(Session()) } }.peek()
    }

    private val renderTargets = mutableListOf<RenderTarget>()
    private val colorBuffers = mutableListOf<ColorBuffer>()
    private val bufferTextures = mutableListOf<BufferTexture>()
    private val vertexBuffers = mutableListOf<VertexBuffer>()
    private val shaders = mutableListOf<Shader>()
    private val cubemaps = mutableListOf<Cubemap>()

    fun track(colorBuffer: ColorBuffer) {
        colorBuffers.add(colorBuffer)
    }

    fun untrack(colorBuffer: ColorBuffer) {
        colorBuffers.remove(colorBuffer)
    }

    fun track(vertexBuffer: VertexBuffer) {
        println("tracking verteBuffer $vertexBuffer")
        vertexBuffers.add(vertexBuffer)
    }

    fun untrack(vertexBuffer: VertexBuffer) {
        vertexBuffers.remove(vertexBuffer)
    }

    fun track(shader: Shader) {
        shaders.add(shader)
    }

    fun untrack(shader: Shader) {
        shaders.remove(shader)
    }

    fun track(cubemap: Cubemap) {
        cubemaps.add(cubemap)
    }

    fun untrack(cubemap: Cubemap) {
        cubemaps.remove(cubemap)
    }

    fun track(bufferTexture: BufferTexture) {
        bufferTextures.add(bufferTexture)
    }

    fun untrack(bufferTexture: BufferTexture) {
        bufferTextures.remove(bufferTexture)
    }


    fun start() {
        sessionStack.get(Driver.instance.contextID)?.push(this)
    }

    fun end() {
        sessionStack.get(Driver.instance.contextID)?.pop()

        logger.debug {
            """
                session ended
                destroying ${renderTargets.size} render targets
                destroying ${colorBuffers.size} color buffers
                destroying ${vertexBuffers.size} vertex buffers
                destroying ${cubemaps.size} cubemaps
                destroying ${bufferTextures.size} buffer textures
            """.trimIndent()
        }

        renderTargets.map { it }.forEach {
            it.detachColorBuffers()
            it.detachDepthBuffer()
            it.destroy()
        }
        colorBuffers.map { it }.forEach {
            it.destroy()
        }

        vertexBuffers.map { it }.forEach {
            it.destroy()
        }

        cubemaps.map { it }.forEach {
            it.destroy()
        }

        bufferTextures.map { it }.forEach {
            it.destroy()
        }

        shaders.map { it }.forEach {
            it.destroy()
        }
    }
}