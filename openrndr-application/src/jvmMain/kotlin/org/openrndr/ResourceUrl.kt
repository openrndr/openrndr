package org.openrndr

import kotlin.reflect.KClass

actual fun resourceUrl(name: String, `class`: KClass<*>): String {
    val resource = `class`::class.java.getResource(name)
    if (resource == null) {
        throw RuntimeException("resource $name not found")
    } else {
        return resource.toExternalForm()
    }
}