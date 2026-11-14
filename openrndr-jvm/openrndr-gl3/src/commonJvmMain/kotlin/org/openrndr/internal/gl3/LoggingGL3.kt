package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KLogger

/**
 * Logs [message] at the trace level. The lambdas of [KLogger.trace] are not inlined, a lambda that captures values is
 * created on every call, also when trace logging is off. This one is only created when trace logging is on.
 */
internal inline fun KLogger.traceIfEnabled(crossinline message: () -> String) {
    if (isTraceEnabled()) {
        trace { message() }
    }
}
