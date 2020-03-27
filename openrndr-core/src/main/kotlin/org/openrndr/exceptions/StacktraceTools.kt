package org.openrndr.exceptions

fun stackRootClassName(thread: Thread = Thread.currentThread(), sanitize: Boolean = true): String {
    val rootClass = Thread.currentThread().stackTrace.last().className
    return if (sanitize) rootClass.replace(Regex("Kt$"), "") else rootClass
}