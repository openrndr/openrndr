package org.openrndr.draw

import mu.KotlinLogging
import org.openrndr.internal.Driver

import java.util.*

private val logger = KotlinLogging.logger {}
private val sessionStack = mutableMapOf<Long, Stack<Session>>()

class SessionStatistics(val renderTargets: Int, val colorBuffers: Int, val depthBuffers: Int, val bufferTextures: Int, val indexBuffers: Int, val vertexBuffers: Int, val shaders: Int, val cubemaps: Int, val arrayTextures: Int, val computeShaders:Int, val atomicCounterBuffers: Int)

class Session(val parent: Session?) {
    val context = Driver.instance.contextID

    companion object {
        val active: Session
            get() = sessionStack.getOrPut(Driver.instance.contextID) { Stack<Session>().apply { push(Session(null)) } }.peek()

        val root: Session
            get() = sessionStack.getOrPut(Driver.instance.contextID) { Stack<Session>().apply { push(Session(null)) } }.first()

        fun endActive() {
            val session = sessionStack.getValue(Driver.instance.contextID).pop()
            session.end()
        }

    }

    private val children = mutableListOf<Session>()

    private val renderTargets = mutableSetOf<RenderTarget>()
    private val colorBuffers = mutableSetOf<ColorBuffer>()
    private val depthBuffers = mutableSetOf<DepthBuffer>()
    private val bufferTextures = mutableSetOf<BufferTexture>()
    private val vertexBuffers = mutableSetOf<VertexBuffer>()
    private val shaders = mutableSetOf<Shader>()
    private val computeShaders = mutableSetOf<ComputeShader>()
    private val cubemaps = mutableSetOf<Cubemap>()
    private val arrayTextures = mutableSetOf<ArrayTexture>()
    private val indexBuffers = mutableSetOf<IndexBuffer>()

    private val atomicCounterBuffers = mutableSetOf<AtomicCounterBuffer>()

    val statistics
        get() =
            SessionStatistics(renderTargets = renderTargets.size,
                    colorBuffers = colorBuffers.size,
                    depthBuffers = depthBuffers.size,
                    bufferTextures = bufferTextures.size,
                    indexBuffers = indexBuffers.size,
                    vertexBuffers = vertexBuffers.size,
                    shaders = shaders.size,
                    cubemaps = cubemaps.size,
                    arrayTextures = arrayTextures.size,
                    computeShaders = computeShaders.size,
                    atomicCounterBuffers = atomicCounterBuffers.size)

    fun track(renderTarget: RenderTarget) = renderTargets.add(renderTarget)
    fun untrack(renderTarget: RenderTarget) = renderTargets.remove(renderTarget)

    fun track(colorBuffer: ColorBuffer) = colorBuffers.add(colorBuffer)
    fun untrack(colorBuffer: ColorBuffer) = colorBuffers.remove(colorBuffer)

    fun track(depthBuffer: DepthBuffer) = depthBuffers.add(depthBuffer)
    fun untrack(depthBuffer: DepthBuffer) = depthBuffers.remove(depthBuffer)

    fun track(vertexBuffer: VertexBuffer) = vertexBuffers.add(vertexBuffer)
    fun untrack(vertexBuffer: VertexBuffer) = vertexBuffers.remove(vertexBuffer)

    fun track(indexBuffer: IndexBuffer) = indexBuffers.add(indexBuffer)
    fun untrack(indexBuffer: IndexBuffer) = indexBuffers.remove(indexBuffer)


    fun track(shader: Shader) = shaders.add(shader)
    fun untrack(shader: Shader) = shaders.remove(shader)

    fun track(computeShader: ComputeShader) = computeShaders.add(computeShader)
    fun untrack(computeShader: ComputeShader) = computeShaders.remove(computeShader)

    fun track(cubemap: Cubemap) = cubemaps.add(cubemap)
    fun untrack(cubemap: Cubemap) = cubemaps.remove(cubemap)

    fun track(bufferTexture: BufferTexture) = bufferTextures.add(bufferTexture)
    fun untrack(bufferTexture: BufferTexture) = bufferTextures.remove(bufferTexture)

    fun track(arrayTexture: ArrayTexture) = arrayTextures.add(arrayTexture)
    fun untrack(arrayTexture: ArrayTexture) = arrayTextures.remove(arrayTexture)


    fun track(atomicCounterBuffer: AtomicCounterBuffer) = atomicCounterBuffers.add(atomicCounterBuffer)
    fun untrack(atomicCounterBuffer: AtomicCounterBuffer) = atomicCounterBuffers.remove(atomicCounterBuffer)

    fun fork(): Session {
        logger.debug { "starting new session for context [id=${context}]" }
        val child = Session(this)
        sessionStack.getValue(Driver.instance.contextID).push(child)
        children.add(child)
        return child
    }

    fun end() {
        val stack = sessionStack.getValue(Driver.instance.contextID)

        parent?.children?.remove(this)

        for (child in children.map { it }) {
            child.end()
        }
        children.clear()

        logger.debug {
            """
                session ended for context [id=${context}]
                destroying ${renderTargets.size} render targets
                destroying ${colorBuffers.size} color buffers
                destroying ${depthBuffers.size} depth buffers
                destroying ${vertexBuffers.size} vertex buffers
                destroying ${cubemaps.size} cubemaps
                destroying ${bufferTextures.size} buffer textures
                destroying ${arrayTextures.size} array textures
                destroying ${computeShaders.size} compute shaders
                destroying ${atomicCounterBuffers.size} atomic counter buffers
            """.trimIndent()
        }

        renderTargets.map { it }.forEach {
            it.detachColorBuffers()
            it.detachDepthBuffer()
            it.destroy()
        }
        renderTargets.clear()

        colorBuffers.map { it }.forEach {
            it.destroy()
        }
        colorBuffers.clear()

        depthBuffers.map { it }.forEach {
            it.destroy()
        }
        depthBuffers.clear()

        vertexBuffers.map { it }.forEach {
            it.destroy()
        }
        vertexBuffers.clear()

        indexBuffers.map { it }.forEach {
            it.destroy()
        }
        indexBuffers.clear()

        cubemaps.map { it }.forEach {
            it.destroy()
        }
        cubemaps.clear()

        bufferTextures.map { it }.forEach {
            it.destroy()
        }
        bufferTextures.clear()

        shaders.map { it }.forEach {
            it.destroy()
        }
        shaders.clear()

        computeShaders.map { it }.forEach {
            it.destroy()
        }
        computeShaders.clear()


        arrayTextures.map { it }.forEach {
            it.destroy()
        }
        arrayTextures.clear()
    }
}

fun session(code: () -> Unit) {
    val s = Session.active.fork()
    code()
    s.end()
}