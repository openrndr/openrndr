package org.openrndr.draw

import org.openrndr.internal.Driver

private val sessionStack = mutableMapOf<Long, ArrayDeque<Session>>()

/**
 * Session statistics
 */
data class SessionStatistics(
    val renderTargets: Int = 0,
    val colorBuffers: Int = 0,
    val depthBuffers: Int = 0,
    val bufferTextures: Int = 0,
    val indexBuffers: Int = 0,
    val vertexBuffers: Int = 0,
    val shaders: Int = 0,
    val cubemaps: Int = 0,
    val arrayTextures: Int = 0,
    val computeShaders: Int = 0,
    val atomicCounterBuffers: Int = 0,
    val arrayCubemaps: Int = 0,
    val shaderStorageBuffers: Int = 0,
    val volumeTextures: Int = 0
) {
    operator fun plus(other: SessionStatistics) = SessionStatistics(
        renderTargets = renderTargets + other.renderTargets,
        colorBuffers = colorBuffers + other.colorBuffers,
        depthBuffers = depthBuffers + other.depthBuffers,
        bufferTextures = bufferTextures + other.bufferTextures,
        indexBuffers = indexBuffers + other.indexBuffers,
        vertexBuffers = vertexBuffers + other.vertexBuffers,
        shaders = shaders + other.shaders,
        cubemaps = cubemaps + other.cubemaps,
        arrayTextures = arrayTextures + other.arrayTextures,
        computeShaders = computeShaders + other.computeShaders,
        atomicCounterBuffers = atomicCounterBuffers + other.atomicCounterBuffers,
        arrayCubemaps = arrayCubemaps + other.arrayCubemaps,
        shaderStorageBuffers = shaderStorageBuffers + other.shaderStorageBuffers,
        volumeTextures = volumeTextures + other.volumeTextures,
    )
}


/**
 * Represents a GPU resource management session. A session is responsible for tracking and managing
 * GPU resources in a hierarchical manner. Sessions can be forked, creating child sessions, and
 * ended, destroying all GPU resources associated with the session.
 *
 * @property parent The parent session, or null if this is a root session.
 */
class Session(val parent: Session?) : AutoCloseable {
    val context = Driver.instance.contextID

    companion object {
        /**
         * The session stack (on the active context)
         */
        val stack: ArrayDeque<Session>
            get() = sessionStack.getOrPut(Driver.instance.contextID) {
                ArrayDeque<Session>().apply {
                    addLast(
                        Session(
                            null
                        )
                    )
                }
            }

        /**
         * The active session (on the active context)
         */
        val active: Session
            get() = stack.last()

        /**
         * The root session (on the active context)
         */
        val root: Session
            get() = stack.first()

        /**
         * Ends the active session and pops it off the session stack (on the active context)
         */
        fun endActive() {
            val session = sessionStack.getValue(Driver.instance.contextID).removeLast()
            session.end()
        }

        /**
         * Returns a sum of the statistics of all sessions in the stack
         */
        val statistics: SessionStatistics
            get() = stack.fold(SessionStatistics()) { acc, session -> acc + session.statistics }
    }

    private val children = mutableListOf<Session>()

    val renderTargets: Set<RenderTarget> = mutableSetOf<RenderTarget>()
    val colorBuffers: Set<ColorBuffer> = mutableSetOf<ColorBuffer>()
    val depthBuffers: Set<DepthBuffer> = mutableSetOf<DepthBuffer>()
    val bufferTextures: Set<BufferTexture> = mutableSetOf<BufferTexture>()
    val vertexBuffers: Set<VertexBuffer> = mutableSetOf<VertexBuffer>()
    val shaders: Set<Shader> = mutableSetOf<Shader>()
    val computeShaders: Set<ComputeShader> = mutableSetOf<ComputeShader>()
    val cubemaps: Set<Cubemap> = mutableSetOf<Cubemap>()
    val arrayTextures: Set<ArrayTexture> = mutableSetOf<ArrayTexture>()
    val arrayCubemaps: Set<ArrayCubemap> = mutableSetOf<ArrayCubemap>()
    val indexBuffers: Set<IndexBuffer> = mutableSetOf<IndexBuffer>()
    val volumeTextures: Set<VolumeTexture> = mutableSetOf<VolumeTexture>()
    val shaderStorageBuffers: Set<ShaderStorageBuffer> = mutableSetOf<ShaderStorageBuffer>()
    val atomicCounterBuffers: Set<AtomicCounterBuffer> = mutableSetOf<AtomicCounterBuffer>()

    /** Session statistics */
    val statistics
        get() = SessionStatistics(
            renderTargets = renderTargets.size,
            colorBuffers = colorBuffers.size,
            depthBuffers = depthBuffers.size,
            bufferTextures = bufferTextures.size,
            indexBuffers = indexBuffers.size,
            vertexBuffers = vertexBuffers.size,
            shaders = shaders.size,
            cubemaps = cubemaps.size,
            arrayTextures = arrayTextures.size,
            computeShaders = computeShaders.size,
            atomicCounterBuffers = atomicCounterBuffers.size,
            arrayCubemaps = arrayCubemaps.size,
            shaderStorageBuffers = shaderStorageBuffers.size,
            volumeTextures = volumeTextures.size
        )

    fun track(renderTarget: RenderTarget) = (renderTargets as MutableSet<RenderTarget>).add(renderTarget)
    fun untrack(renderTarget: RenderTarget) = (renderTargets as MutableSet<RenderTarget>).remove(renderTarget)

    fun track(colorBuffer: ColorBuffer) = (colorBuffers as MutableSet<ColorBuffer>).add(colorBuffer)
    fun untrack(colorBuffer: ColorBuffer) = (colorBuffers as MutableSet<ColorBuffer>).remove(colorBuffer)

    fun track(depthBuffer: DepthBuffer) = (depthBuffers as MutableSet<DepthBuffer>).add(depthBuffer)
    fun untrack(depthBuffer: DepthBuffer) = (depthBuffers as MutableSet<DepthBuffer>).remove(depthBuffer)

    fun track(vertexBuffer: VertexBuffer) = (vertexBuffers as MutableSet<VertexBuffer>).add(vertexBuffer)
    fun untrack(vertexBuffer: VertexBuffer) = (vertexBuffers as MutableSet<VertexBuffer>).remove(vertexBuffer)

    fun track(indexBuffer: IndexBuffer) = (indexBuffers as MutableSet<IndexBuffer>).add(indexBuffer)
    fun untrack(indexBuffer: IndexBuffer) = (indexBuffers as MutableSet<IndexBuffer>).remove(indexBuffer)

    fun track(shader: Shader) = (shaders as MutableSet<Shader>).add(shader)
    fun untrack(shader: Shader) = (shaders as MutableSet<Shader>).remove(shader)

    fun track(computeShader: ComputeShader) = (computeShaders as MutableSet<ComputeShader>).add(computeShader)
    fun untrack(computeShader: ComputeShader) = (computeShaders as MutableSet<ComputeShader>).remove(computeShader)

    fun track(cubemap: Cubemap) = (cubemaps as MutableSet<Cubemap>).add(cubemap)
    fun untrack(cubemap: Cubemap) = (cubemaps as MutableSet<Cubemap>).remove(cubemap)

    fun track(bufferTexture: BufferTexture) = (bufferTextures as MutableSet<BufferTexture>).add(bufferTexture)
    fun untrack(bufferTexture: BufferTexture) = (bufferTextures as MutableSet<BufferTexture>).remove(bufferTexture)

    fun track(arrayTexture: ArrayTexture) = (arrayTextures as MutableSet<ArrayTexture>).add(arrayTexture)
    fun untrack(arrayTexture: ArrayTexture) = (arrayTextures as MutableSet<ArrayTexture>).remove(arrayTexture)

    fun track(atomicCounterBuffer: AtomicCounterBuffer) = (atomicCounterBuffers as MutableSet<AtomicCounterBuffer>).add(atomicCounterBuffer)
    fun untrack(atomicCounterBuffer: AtomicCounterBuffer) = (atomicCounterBuffers as MutableSet<AtomicCounterBuffer>).remove(atomicCounterBuffer)

    fun track(arrayCubemap: ArrayCubemap) = (arrayCubemaps as MutableSet<ArrayCubemap>).add(arrayCubemap)
    fun untrack(arrayCubemap: ArrayCubemap) = (arrayCubemaps as MutableSet<ArrayCubemap>).remove(arrayCubemap)

    fun track(volumeTexture: VolumeTexture) = (volumeTextures as MutableSet<VolumeTexture>).add(volumeTexture)
    fun untrack(volumeTexture: VolumeTexture) = (volumeTextures as MutableSet<VolumeTexture>).remove(volumeTexture)

    fun track(shaderStorageBuffer: ShaderStorageBuffer) = (shaderStorageBuffers as MutableSet<ShaderStorageBuffer>).add(shaderStorageBuffer)
    fun untrack(shaderStorageBuffer: ShaderStorageBuffer) = (shaderStorageBuffers as MutableSet<ShaderStorageBuffer>).remove(shaderStorageBuffer)

    /**
     * Fork the session
     */
    fun fork(): Session {
        val child = Session(this)
        sessionStack.getValue(Driver.instance.contextID).addLast(child)
        children.add(child)
        return child
    }

    /**
     * Ends the session, destroys any GPU resources in use by the session
     */
    fun end() {
        require(Driver.instance.contextID == context)

        parent?.children?.remove(this)

        for (child in children.map { it }) {
            child.end()
        }
        children.clear()

        renderTargets as MutableSet<RenderTarget>
        renderTargets.map { it }.forEach {
            it.detachColorAttachments()
            it.detachDepthBuffer()
            it.destroy()
        }
        renderTargets.clear()

        colorBuffers as MutableSet<ColorBuffer>
        colorBuffers.map { it }.forEach {
            it.destroy()
        }
        colorBuffers.clear()

        depthBuffers as MutableSet<DepthBuffer>
        depthBuffers.map { it }.forEach {
            it.destroy()
        }
        depthBuffers.clear()

        vertexBuffers as MutableSet<VertexBuffer>
        vertexBuffers.map { it }.forEach {
            it.destroy()
        }
        vertexBuffers.clear()

        indexBuffers as MutableSet<IndexBuffer>
        indexBuffers.map { it }.forEach {
            it.destroy()
        }
        indexBuffers.clear()

        cubemaps as MutableSet<Cubemap>
        cubemaps.map { it }.forEach {
            it.destroy()
        }
        cubemaps.clear()

        bufferTextures as MutableSet<BufferTexture>
        bufferTextures.map { it }.forEach {
            it.destroy()
        }
        bufferTextures.clear()

        shaders as MutableSet<Shader>
        shaders.map { it }.forEach {
            it.destroy()
        }
        shaders.clear()

        computeShaders as MutableSet<ComputeShader>
        computeShaders.map { it }.forEach {
            it.destroy()
        }
        computeShaders.clear()

        arrayTextures as MutableSet<ArrayTexture>
        arrayTextures.map { it }.forEach {
            it.destroy()
        }
        arrayTextures.clear()

        arrayCubemaps as MutableSet<ArrayCubemap>
        arrayCubemaps.map { it }.forEach {
            it.destroy()
        }
        arrayCubemaps.clear()

        shaderStorageBuffers as MutableSet<ShaderStorageBuffer>
        shaderStorageBuffers.map { it }.forEach {
            it.destroy()
        }
        shaderStorageBuffers.clear()

        volumeTextures as MutableSet<VolumeTexture>
        volumeTextures.map { it }.forEach {
            it.destroy()
        }
        volumeTextures.clear()
    }

    fun pop() {
        val top = sessionStack.getValue(Driver.instance.contextID).last()
        require(top == this) {
            error("attempted to pop session that is not active")
        }
        sessionStack.getValue(Driver.instance.contextID).removeLast()
    }

    fun push() {
        val top = sessionStack.getValue(Driver.instance.contextID).last()
        require(top != this) {
            error("attempted to push session that is already active")
        }
        sessionStack.getValue(Driver.instance.contextID).addLast(this)
    }

    override fun close() {
        end()
    }
}

/** Runs code inside a (short-lived) session */
fun session(code: () -> Unit) {
    val s = Session.active.fork()
    code()
    s.end()
}

/**
 * Mark a GPU resource or code that uses GPU resources as persistent
 */
fun <T> persistent(builder: () -> T): T {
    Session.stack.addLast(Session.root)
    val result = builder()
    Session.stack.removeLast()
    return result
}