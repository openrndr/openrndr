package org.openrndr.platform
actual object Platform {
    actual val type: PlatformType
        get() = TODO("Not yet implemented")
    actual val architecture: PlatformArchitecture
        get() = TODO("Not yet implemented")

    actual fun property(key: String): String? {
        TODO("Not yet implemented")
    }

}