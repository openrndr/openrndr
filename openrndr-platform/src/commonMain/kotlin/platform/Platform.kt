package org.openrndr.platform

enum class PlatformType {
    GENERIC,
    WINDOWS,
    MAC,
    BROWSER
}

enum class PlatformArchitecture {
    X86_64,
    AARCH64,
    UNKNOWN
}

expect object Platform {
    val type: PlatformType
    val architecture: PlatformArchitecture
    fun property(key: String): String?
}