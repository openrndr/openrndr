package org.openrndr.platform

actual object Platform {

    actual val type: PlatformType
        get() = PlatformType.ANDROID

    actual val architecture: PlatformArchitecture
        get() = PlatformArchitecture.ARMV8A

    actual fun property(key: String): String? {
        return null
    }

}