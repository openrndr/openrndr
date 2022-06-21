package org.openrndr

/**
 * Creates and runs a synchronous OPENRNDR application using the provided [ApplicationBuilder].
 * @see <a href="https://guide.openrndr.org/">the OPENRNDR guide</a>
 */
actual fun application(build: ApplicationBuilder.() -> Unit) {
    throw NotImplementedError("Synchronous application is unsupported, use applicationAsync()")
}

/**
 * Creates and runs an asynchronous OPENRNDR application using the provided [ApplicationBuilder].
 * @see <a href="https://guide.openrndr.org/">the OPENRNDR guide</a>
 */
actual suspend fun applicationAsync(build: ApplicationBuilder.() -> Unit) {
    ApplicationBuilder().apply {
        build()
        application.build(program, configuration).runAsync()
    }
}

@Suppress("DeprecatedCallableAddReplaceWith")
@ApplicationDslMarker
actual class ApplicationBuilder internal actual constructor() {
    internal actual val configuration = Configuration()
    actual var program: Program = Program()
    internal actual val application = applicationBaseFunc?.invoke() ?: error("applicationFunc not set")

    actual fun configure(init: Configuration.() -> Unit) {
        configuration.init()
    }

    actual fun program(init: suspend Program.() -> Unit) {
        program = object : Program() {
            override suspend fun setup() {
                init()
            }
        }
    }

    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    actual fun application(build: ApplicationBuilder.() -> Unit): Nothing = error("Cannot construct application in an application block.")

    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    actual fun applicationAsync(build: ApplicationBuilder.() -> Unit): Nothing = error("Cannot construct application in an application block.")

    @Deprecated("Cannot construct program in a program block.", level = DeprecationLevel.ERROR)
    actual fun Program.program(init: Program.() -> Unit): Nothing = error("Cannot construct program in a program block.")
}
