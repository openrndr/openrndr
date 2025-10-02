package org.openrndr.draw

import java.io.File

fun probeImage(file: File): ImageFileDetails? {
    return probeImage(file.absolutePath)
}
