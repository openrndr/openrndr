package org.openrndr.draw

interface ShaderBufferBindings {
    fun buffer(name:String, vertexBuffer: VertexBuffer)
    fun buffer(name:String, shaderStorageBuffer: ShaderStorageBuffer)
    fun buffer(name:String, counterBuffer: AtomicCounterBuffer)
}