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
        application.runAsync(program, configuration)
    }
}

@ApplicationDslMarker
actual class ApplicationBuilder internal actual constructor(){
    internal actual val configuration = Configuration()
    actual var program: Program = Program()
    actual val application = applicationFunc?.invoke(program, configuration) ?: error("applicationFunc not set")

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
}
