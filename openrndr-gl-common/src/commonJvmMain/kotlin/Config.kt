package org.openrndr.internal.glcommon

actual val ignoreShadeStyleErrors: Boolean
    get() = System.getProperties().containsKey("org.openrndr.ignoreShadeStyleErrors")