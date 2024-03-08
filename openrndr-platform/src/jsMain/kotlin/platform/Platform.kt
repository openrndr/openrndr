package org.openrndr.platform

actual object Platform {
    actual val type: PlatformType
        get() = PlatformType.BROWSER

    actual val architecture: PlatformArchitecture
        get() = PlatformArchitecture.UNKNOWN

    actual fun property(key: String): String? {
        return null
    }
}