package org.openrndr.math.test

import kotlin.test.fail


fun it(description: String, block: () -> Unit) = try {
    block()
} catch (e: AssertionError) {
    fail("Expected that $description: ${e.message}", e)
}

inline fun <T> T.should(message: String, assertion: T.() -> Boolean): T =
    if (assertion()) this else fail(message)
