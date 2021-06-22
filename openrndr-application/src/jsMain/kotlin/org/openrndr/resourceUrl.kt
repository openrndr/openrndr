package org.openrndr

import kotlin.reflect.KClass

actual fun resourceUrl(name: String, `class`: KClass<*>): String {
    error("not supported")
}

actual fun resourceText(name: String, `class`: KClass<*>): String {
    error("not supported")
}

