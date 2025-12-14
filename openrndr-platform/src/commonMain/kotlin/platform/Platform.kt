package org.openrndr.platform

enum class PlatformType {
    GENERIC,
    WINDOWS,
    MAC,
    BROWSER,
    ANDROID
}

enum class PlatformArchitecture {
    X86_64,
    AARCH64,
    ARMV8A,
    UNKNOWN
}

expect object Platform {
    val type: PlatformType
    val architecture: PlatformArchitecture
    fun property(key: String): String?
}