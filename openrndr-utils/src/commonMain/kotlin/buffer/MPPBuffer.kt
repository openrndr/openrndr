package org.openrndr.utils.buffer

expect class MPPBuffer {
    fun rewind()
    val int: Int
    val float: Float
    val double: Double
    val byte: Byte

    fun get(): Byte
    fun capacity(): Int

    fun get(target: ByteArray)
    fun remaining() : Int

    fun limit(): Int
    fun limit(newLimit: Int)
    fun position(): Int
    fun position(newPosition: Int)
    fun flip()


    fun put(byte: Byte)
    fun put(ubyte: UByte)
    fun put(buffer: MPPBuffer)

    fun put(short: Short)
    fun put(ushort: UShort)
        
    fun putFloat(float: Float)
    fun putDouble(double: Double)
    fun putInt(int: Int)

    companion object {
        fun allocate(size: Int) : MPPBuffer
        fun createFrom(fromBytes: ByteArray) : MPPBuffer
    }
}