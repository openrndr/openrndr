package org.openrndr.ktessellation

fun <T> arraycopy(src: Array<T>, srcPos: Int, dest: Array<T>, destPos: Int, length: Int) {
    for (i in 0 until length) {
        if (i + srcPos >= src.size || i + destPos >= dest.size)
            break
        dest[i + destPos] = src[i + srcPos]
    }
}

fun arraycopy(src: FloatArray, srcPos: Int, dest: FloatArray, destPos: Int, length: Int) {
    for (i in 0 until length) {
        if (i + srcPos >= src.size || i + destPos >= dest.size)
            break
        dest[i + destPos] = src[i + srcPos]
    }
}


