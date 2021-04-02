package org.openrndr.platform

actual object Platform {
    actual val type: PlatformType
        get() = PlatformType.BROWSER
}