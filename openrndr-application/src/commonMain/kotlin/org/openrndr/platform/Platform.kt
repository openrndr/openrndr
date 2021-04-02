package org.openrndr.platform



enum class PlatformType {
    GENERIC,
    WINDOWS,
    MAC,
    BROWSER
}

expect object Platform {
    val type: PlatformType

}